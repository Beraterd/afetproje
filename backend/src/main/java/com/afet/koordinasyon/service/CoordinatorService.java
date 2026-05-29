package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.dto.request.UpdateCoordinatorLocationRequest;
import com.afet.koordinasyon.dto.response.CoordinatorDistrictResponse;
import com.afet.koordinasyon.dto.response.CoordinatorNeighborhoodResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.TeamMembershipRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.email.DistrictCoordinationCenterSetEmailEvent;
import com.afet.koordinasyon.service.email.DistrictCoordinatorAssignedEmailEvent;
import com.afet.koordinasyon.service.email.NeighborhoodCoordinationCenterSetEmailEvent;
import com.afet.koordinasyon.service.email.NeighborhoodCoordinatorAssignedEmailEvent;
import com.afet.koordinasyon.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoordinatorService {

    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CoordinatorDistrictResponse> listDistrictCoordinators() {
        return districtRepository.findByActiveTrue().stream()
                .map(this::toDistrictResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CoordinatorNeighborhoodResponse> listNeighborhoodCoordinators(UUID districtId, UserPrincipal actor) {
        UUID effectiveDistrictId;
        if (actor.getRole() == UserRole.DISTRICT_COORDINATOR) {
            // Use the district stored on the user's own profile (user.district_id).
            // This is reliable even when district.coordinator FK is not set via the coordinator-assignment flow.
            effectiveDistrictId = actor.getDistrictId();
            if (effectiveDistrictId == null) {
                log.warn("listNeighborhoodCoordinators: DISTRICT_COORDINATOR {} has no districtId in token — returning empty list",
                        actor.getId());
                return List.of();
            }
        } else {
            effectiveDistrictId = districtId;
        }
        List<Neighborhood> neighborhoods = effectiveDistrictId != null
                ? neighborhoodRepository.findByDistrictId(effectiveDistrictId)
                : neighborhoodRepository.findAll();
        return neighborhoods.stream()
                .map(this::toNeighborhoodResponse)
                .toList();
    }

    /**
     * Returns users who are eligible to be assigned as coordinators.
     * İkamet şartı yoktur: herhangi bir kullanıcı herhangi bir ilçe/mahalleye koordinatör atanabilir.
     * Atanabilir olmak için:
     *   - ADMIN rolünde olmamalı
     *   - Zaten aktif DISTRICT_COORDINATOR veya NEIGHBORHOOD_COORDINATOR olmamalı
     *   - Aktif ekip üyeliği olmamalı
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listEligibleUsers(UserPrincipal actor, String assignmentType, UUID districtId) {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(u -> u.getRole() != UserRole.ADMIN)
                .filter(u -> u.getRole() != UserRole.DISTRICT_COORDINATOR)
                .filter(u -> u.getRole() != UserRole.NEIGHBORHOOD_COORDINATOR)
                .filter(u -> !teamMembershipRepository.existsByUserIdAndActiveTrue(u.getId()))
                .map(AuthService::mapUserToResponse)
                .toList();
    }

    @Transactional
    public CoordinatorDistrictResponse assignDistrictCoordinator(UUID districtId, UUID userId, UserPrincipal actor) {
        if (actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleException("Sadece yöneticiler ilçe koordinatörü atayabilir");
        }
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Eligibility checks
        if (user.getRole() == UserRole.DISTRICT_COORDINATOR
                && (district.getCoordinator() == null || !district.getCoordinator().getId().equals(userId))) {
            throw new BusinessRuleException("Bu kullanıcı zaten başka bir ilçede koordinatör olarak görev yapıyor");
        }
        if (user.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR) {
            throw new BusinessRuleException("Bu kullanıcı zaten aktif bir mahalle koordinatörüdür; önce bu görevi sonlandırın");
        }
        if (teamMembershipRepository.existsByUserIdAndActiveTrue(userId)) {
            throw new BusinessRuleException("Aktif ekip üyeliği olan bir kullanıcı koordinatör olarak atanamaz");
        }

        // Demote the current coordinator of this district (if different) back to VOLUNTEER
        if (district.getCoordinator() != null && !district.getCoordinator().getId().equals(userId)) {
            User oldCoord = district.getCoordinator();
            if (oldCoord.getRole() == UserRole.DISTRICT_COORDINATOR) {
                oldCoord.setRole(UserRole.VOLUNTEER);
                userRepository.save(oldCoord);
            }
        }

        // If the new user was previously coordinator of a different district, clear that reference
        districtRepository.findByCoordinatorId(userId).ifPresent(prevDistrict -> {
            if (!prevDistrict.getId().equals(districtId)) {
                prevDistrict.setCoordinator(null);
                districtRepository.save(prevDistrict);
            }
        });

        // If the new user was previously coordinator of a neighborhood, clear that reference
        neighborhoodRepository.findByCoordinatorId(userId).ifPresent(prevNeighborhood -> {
            prevNeighborhood.setCoordinator(null);
            neighborhoodRepository.save(prevNeighborhood);
        });

        // Promote user to district coordinator.
        // user.district / user.neighborhood are residence fields — do NOT touch them.
        // The coordinator assignment is tracked via district.coordinator FK only.
        user.setRole(UserRole.DISTRICT_COORDINATOR);
        userRepository.save(user);

        district.setCoordinator(user);
        districtRepository.save(district);
        auditLogService.logCoordinatorAssigned(actor, userId,
                user.getFirstName() + " " + user.getLastName(), "DISTRICT", district.getName());
        eventPublisher.publishEvent(new DistrictCoordinatorAssignedEmailEvent(districtId, userId));
        return toDistrictResponse(district);
    }

    @Transactional
    public CoordinatorNeighborhoodResponse assignNeighborhoodCoordinator(UUID neighborhoodId, UUID userId, UserPrincipal actor) {
        Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId)
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", neighborhoodId));

        UUID targetNeighborhoodDistrictId = neighborhood.getDistrict().getId();

        log.info("Assign neighborhood coordinator check: actorUserId={}, actorRole={}, actorDistrictId={}, " +
                        "targetNeighborhoodId={}, targetNeighborhoodDistrictId={}, assigneeUserId={}",
                actor.getId(), actor.getRole(), actor.getDistrictId(),
                neighborhoodId, targetNeighborhoodDistrictId, userId);

        if (actor.getRole() == UserRole.DISTRICT_COORDINATOR) {
            UUID actorDistrictId = actor.getDistrictId();
            if (actorDistrictId == null) {
                throw new BusinessRuleException("İlçe koordinatörü hesabınızda ilçe bilgisi bulunmuyor");
            }
            if (!actorDistrictId.equals(targetNeighborhoodDistrictId)) {
                throw new BusinessRuleException("Sadece kendi ilçenizdeki mahallelere koordinatör atayabilirsiniz");
            }
        } else if (actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleException("Bu işlem için yetkiniz bulunmamaktadır");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Eligibility checks
        if (user.getRole() == UserRole.DISTRICT_COORDINATOR) {
            throw new BusinessRuleException("Bu kullanıcı zaten aktif bir ilçe koordinatörüdür; önce bu görevi sonlandırın");
        }
        if (user.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR
                && (neighborhood.getCoordinator() == null || !neighborhood.getCoordinator().getId().equals(userId))) {
            throw new BusinessRuleException("Bu kullanıcı zaten başka bir mahallede koordinatör olarak görev yapıyor");
        }
        if (teamMembershipRepository.existsByUserIdAndActiveTrue(userId)) {
            throw new BusinessRuleException("Aktif ekip üyeliği olan bir kullanıcı koordinatör olarak atanamaz");
        }

        // Demote the current coordinator of this neighborhood (if different) back to VOLUNTEER
        if (neighborhood.getCoordinator() != null && !neighborhood.getCoordinator().getId().equals(userId)) {
            User oldCoord = neighborhood.getCoordinator();
            if (oldCoord.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR) {
                oldCoord.setRole(UserRole.VOLUNTEER);
                userRepository.save(oldCoord);
            }
        }

        // If the new user was previously coordinator of a district, clear that reference
        districtRepository.findByCoordinatorId(userId).ifPresent(prevDistrict -> {
            prevDistrict.setCoordinator(null);
            districtRepository.save(prevDistrict);
        });

        // If the new user was previously coordinator of a different neighborhood, clear that reference
        neighborhoodRepository.findByCoordinatorId(userId).ifPresent(prevNeighborhood -> {
            if (!prevNeighborhood.getId().equals(neighborhoodId)) {
                prevNeighborhood.setCoordinator(null);
                neighborhoodRepository.save(prevNeighborhood);
            }
        });

        // Promote user to neighborhood coordinator.
        // user.district / user.neighborhood are residence fields — do NOT touch them.
        // The coordinator assignment is tracked via neighborhood.coordinator FK only.
        user.setRole(UserRole.NEIGHBORHOOD_COORDINATOR);
        userRepository.save(user);

        neighborhood.setCoordinator(user);
        neighborhoodRepository.save(neighborhood);
        auditLogService.logCoordinatorAssigned(actor, userId,
                user.getFirstName() + " " + user.getLastName(), "NEIGHBORHOOD", neighborhood.getName());
        eventPublisher.publishEvent(new NeighborhoodCoordinatorAssignedEmailEvent(neighborhoodId, userId));
        return toNeighborhoodResponse(neighborhood);
    }

    @Transactional
    public CoordinatorDistrictResponse updateDistrictCoordinatorLocation(
            UUID districtId, UpdateCoordinatorLocationRequest req, UserPrincipal actor) {

        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));

        // Authorization: ADMIN can update any district; DISTRICT_COORDINATOR only own
        if (actor.getRole() == UserRole.DISTRICT_COORDINATOR) {
            if (!districtId.equals(actor.getDistrictId())) {
                throw new BusinessRuleException("Sadece kendi ilçenizin koordinatör konumunu güncelleyebilirsiniz");
            }
        } else if (actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleException("Bu işlem için yetkiniz bulunmamaktadır");
        }

        // Polygon boundary check
        if (!GeoUtils.isPointInPolygon(req.getLatitude(), req.getLongitude(), district.getGeojsonPolygon())) {
            throw new BusinessRuleException("Seçilen konum ilçe sınırları dışında. Lütfen ilçe içinde bir konum seçin.");
        }

        User updatedBy = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", actor.getId()));

        district.setCoordinatorLatitude(BigDecimal.valueOf(req.getLatitude()));
        district.setCoordinatorLongitude(BigDecimal.valueOf(req.getLongitude()));
        district.setCoordinatorLocationUpdatedAt(OffsetDateTime.now());
        district.setCoordinatorLocationUpdatedBy(updatedBy);
        districtRepository.save(district);
        eventPublisher.publishEvent(new DistrictCoordinationCenterSetEmailEvent(districtId));

        return toDistrictResponse(district);
    }

    @Transactional
    public CoordinatorNeighborhoodResponse updateNeighborhoodCoordinatorLocation(
            UUID neighborhoodId, UpdateCoordinatorLocationRequest req, UserPrincipal actor) {

        Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId)
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", neighborhoodId));

        // Authorization
        if (actor.getRole() == UserRole.DISTRICT_COORDINATOR) {
            if (!neighborhood.getDistrict().getId().equals(actor.getDistrictId())) {
                throw new BusinessRuleException("Sadece kendi ilçenizdeki mahallelerin koordinatör konumunu güncelleyebilirsiniz");
            }
        } else if (actor.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR) {
            if (!neighborhoodId.equals(actor.getNeighborhoodId())) {
                throw new BusinessRuleException("Sadece kendi mahallenizin koordinatör konumunu güncelleyebilirsiniz");
            }
        } else if (actor.getRole() != UserRole.ADMIN) {
            throw new BusinessRuleException("Bu işlem için yetkiniz bulunmamaktadır");
        }

        // Polygon boundary check
        if (!GeoUtils.isPointInPolygon(req.getLatitude(), req.getLongitude(), neighborhood.getGeojsonPolygon())) {
            throw new BusinessRuleException("Seçilen konum mahalle sınırları dışında. Lütfen mahalle içinde bir konum seçin.");
        }

        User updatedBy = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", actor.getId()));

        neighborhood.setCoordinatorLatitude(BigDecimal.valueOf(req.getLatitude()));
        neighborhood.setCoordinatorLongitude(BigDecimal.valueOf(req.getLongitude()));
        neighborhood.setCoordinatorLocationUpdatedAt(OffsetDateTime.now());
        neighborhood.setCoordinatorLocationUpdatedBy(updatedBy);
        neighborhoodRepository.save(neighborhood);
        eventPublisher.publishEvent(new NeighborhoodCoordinationCenterSetEmailEvent(neighborhoodId));

        return toNeighborhoodResponse(neighborhood);
    }

    private CoordinatorDistrictResponse toDistrictResponse(District d) {
        User coord = d.getCoordinator();
        return CoordinatorDistrictResponse.builder()
                .districtId(d.getId())
                .districtName(d.getName())
                .coordinatorId(coord != null ? coord.getId() : null)
                .coordinatorFirstName(coord != null ? coord.getFirstName() : null)
                .coordinatorLastName(coord != null ? coord.getLastName() : null)
                .coordinatorEmail(coord != null ? coord.getEmail() : null)
                .build();
    }

    private CoordinatorNeighborhoodResponse toNeighborhoodResponse(Neighborhood n) {
        User coord = n.getCoordinator();
        District d = n.getDistrict();
        return CoordinatorNeighborhoodResponse.builder()
                .neighborhoodId(n.getId())
                .neighborhoodName(n.getName())
                .districtId(d != null ? d.getId() : null)
                .districtName(d != null ? d.getName() : null)
                .coordinatorId(coord != null ? coord.getId() : null)
                .coordinatorFirstName(coord != null ? coord.getFirstName() : null)
                .coordinatorLastName(coord != null ? coord.getLastName() : null)
                .coordinatorEmail(coord != null ? coord.getEmail() : null)
                .build();
    }
}
