package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.UpdateNotificationPreferencesRequest;
import com.afet.koordinasyon.dto.request.UpdateProfileRequest;
import com.afet.koordinasyon.dto.response.NotificationPreferencesResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import java.util.List;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ConflictException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.util.PhoneNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(int page, int size, String role) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
        Page<User> users;
        if (role != null && !role.isBlank()) {
            try {
                UserRole userRole = UserRole.valueOf(role);
                users = userRepository.findByRole(userRole, pageable);
            } catch (IllegalArgumentException e) {
                users = userRepository.findAll(pageable);
            }
        } else {
            users = userRepository.findAll(pageable);
        }
        return PagedResponse.from(users.map(AuthService::mapUserToResponse));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Bu e-posta adresi zaten kullanımda");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            // Telefon DB standardı E.164 (+905XXXXXXXXX); benzersizlik kontrolü normalize değerle.
            String normalizedPhone = PhoneNumberUtil.toE164(request.getPhone());
            if (!normalizedPhone.equals(user.getPhone())
                    && userRepository.existsByPhone(normalizedPhone)) {
                throw new ConflictException("Bu telefon numarası zaten kullanımda");
            }
            user.setPhone(normalizedPhone);
        }
        if (request.getBloodType() != null) {
            user.setBloodType(request.getBloodType());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getProfession() != null) {
            user.setProfession(request.getProfession());
        }
        if (request.getDistrictId() != null) {
            District district = districtRepository.findById(request.getDistrictId())
                    .orElseThrow(() -> new ResourceNotFoundException("District", "id", request.getDistrictId()));
            user.setDistrict(district);
            if (request.getNeighborhoodId() != null) {
                Neighborhood neighborhood = neighborhoodRepository.findById(request.getNeighborhoodId())
                        .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", request.getNeighborhoodId()));
                if (!neighborhood.getDistrict().getId().equals(request.getDistrictId())) {
                    throw new BusinessRuleException("Seçilen mahalle bu ilçeye ait değil");
                }
                user.setNeighborhood(neighborhood);
            } else {
                user.setNeighborhood(null);
            }
        }

        return AuthService.mapUserToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse assignRole(UUID userId, UserRole newRole, UserPrincipal actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String oldRole = user.getRole().name();
        user.setRole(newRole);
        UserResponse response = AuthService.mapUserToResponse(userRepository.save(user));
        auditLogService.logRoleChange(actor, userId,
                user.getFirstName() + " " + user.getLastName(), oldRole, newRole.name());
        return response;
    }

    @Transactional(readOnly = true)
    public List<com.afet.koordinasyon.dto.response.UserSearchResponse> searchUsers(String query) {
        if (query == null || query.isBlank() || query.length() < 2) {
            return List.of();
        }
        return userRepository.searchByQuery(query.trim(),
                PageRequest.of(0, 10, Sort.by("lastName", "firstName")))
                .stream()
                .map(u -> com.afet.koordinasyon.dto.response.UserSearchResponse.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .build())
                .toList();
    }

    @Transactional
    public void deactivateUser(UUID userId, UserPrincipal actor) {
        if (userId.equals(actor.getId())) {
            throw new BusinessRuleException("Kendi hesabınızı devre dışı bırakamazsınız");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(false);
        userRepository.save(user);
        auditLogService.logUserAction(actor, AuditActionType.USER_DEACTIVATED, "USER", userId,
                user.getFirstName() + " " + user.getLastName() + " kullanıcısı devre dışı bırakıldı", null);
    }

    @Transactional
    public void activateUser(UUID userId, UserPrincipal actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setActive(true);
        userRepository.save(user);
        auditLogService.logUserAction(actor, AuditActionType.USER_ACTIVATED, "USER", userId,
                user.getFirstName() + " " + user.getLastName() + " kullanıcısı aktif edildi", null);
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getNotificationPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toPrefsResponse(user);
    }

    @Transactional
    public NotificationPreferencesResponse updateNotificationPreferences(UUID userId,
                                                                          UpdateNotificationPreferencesRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (req.getEmailTaskNotificationsEnabled() != null)
            user.setEmailTaskNotificationsEnabled(req.getEmailTaskNotificationsEnabled());
        if (req.getEmailDamageNotificationsEnabled() != null)
            user.setEmailDamageNotificationsEnabled(req.getEmailDamageNotificationsEnabled());
        if (req.getEmailEarthquakeNotificationsEnabled() != null)
            user.setEmailEarthquakeNotificationsEnabled(req.getEmailEarthquakeNotificationsEnabled());
        if (req.getEmailTeamNotificationsEnabled() != null)
            user.setEmailTeamNotificationsEnabled(req.getEmailTeamNotificationsEnabled());
        if (req.getEmailAidNotificationsEnabled() != null)
            user.setEmailAidNotificationsEnabled(req.getEmailAidNotificationsEnabled());
        if (req.getEmailSystemNotificationsEnabled() != null)
            user.setEmailSystemNotificationsEnabled(req.getEmailSystemNotificationsEnabled());
        return toPrefsResponse(userRepository.save(user));
    }

    private static NotificationPreferencesResponse toPrefsResponse(User user) {
        return NotificationPreferencesResponse.builder()
                .emailTaskNotificationsEnabled(user.isEmailTaskNotificationsEnabled())
                .emailDamageNotificationsEnabled(user.isEmailDamageNotificationsEnabled())
                .emailEarthquakeNotificationsEnabled(user.isEmailEarthquakeNotificationsEnabled())
                .emailTeamNotificationsEnabled(user.isEmailTeamNotificationsEnabled())
                .emailAidNotificationsEnabled(user.isEmailAidNotificationsEnabled())
                .emailSystemNotificationsEnabled(user.isEmailSystemNotificationsEnabled())
                .build();
    }
}
