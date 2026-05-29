package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.*;
import com.afet.koordinasyon.domain.enums.*;
import com.afet.koordinasyon.dto.request.CreateResourceRequestRequest;
import com.afet.koordinasyon.dto.request.UpdateResourceStatusRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.service.email.ResourceRequestCreatedEmailEvent;
import com.afet.koordinasyon.service.email.ResourceRequestStatusChangedEmailEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceRequestService {

    private final ResourceRequestRepository resourceRequestRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagedResponse<ResourceRequestResponse> listRequests(
            int page, int size, UUID districtId, UUID neighborhoodId,
            String status, UserPrincipal principal) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (principal.getRole() == UserRole.DISTRICT_COORDINATOR && districtId == null) {
            districtId = principal.getDistrictId();
        }
        if (principal.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR && neighborhoodId == null) {
            neighborhoodId = principal.getNeighborhoodId();
        }

        Page<ResourceRequest> result;
        if (neighborhoodId != null) {
            result = resourceRequestRepository.findByNeighborhoodId(neighborhoodId, pageable);
        } else if (districtId != null) {
            result = resourceRequestRepository.findByDistrictId(districtId, pageable);
        } else {
            result = resourceRequestRepository.findAll(pageable);
        }

        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ResourceRequestResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ResourceRequestResponse create(CreateResourceRequestRequest req, UserPrincipal principal) {
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new BusinessRuleException("Gönüllüler kaynak talebi oluşturamaz");
        }

        District district = districtRepository.findById(req.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", req.getDistrictId()));

        Neighborhood neighborhood = null;
        if (req.getNeighborhoodId() != null) {
            neighborhood = neighborhoodRepository.findById(req.getNeighborhoodId())
                    .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", req.getNeighborhoodId()));
            if (!neighborhood.getDistrict().getId().equals(district.getId())) {
                throw new BusinessRuleException("Seçilen mahalle bu ilçeye ait değil");
            }
        }

        // Yetki kontrolü
        if (role == UserRole.DISTRICT_COORDINATOR) {
            if (!district.getId().equals(principal.getDistrictId())) {
                throw new BusinessRuleException("Sadece kendi ilçeniz için talep oluşturabilirsiniz");
            }
        } else if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            if (!district.getId().equals(principal.getDistrictId())) {
                throw new BusinessRuleException("Sadece kendi ilçeniz için talep oluşturabilirsiniz");
            }
            if (neighborhood == null || !neighborhood.getId().equals(principal.getNeighborhoodId())) {
                throw new BusinessRuleException("Sadece kendi mahalleniz için talep oluşturabilirsiniz");
            }
        }

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        RequestScope scope = (neighborhood != null) ? RequestScope.NEIGHBORHOOD : RequestScope.DISTRICT;

        ResourceRequest saved = resourceRequestRepository.save(ResourceRequest.builder()
                .district(district)
                .neighborhood(neighborhood)
                .requestScope(scope)
                .resourceType(req.getResourceType())
                .quantity(req.getQuantity())
                .description(req.getDescription())
                .createdBy(creator)
                .build());

        eventPublisher.publishEvent(new ResourceRequestCreatedEmailEvent(saved.getId(), creator.getId()));
        return toResponse(saved);
    }

    @Transactional
    public ResourceRequestResponse updateStatus(UUID id, UpdateResourceStatusRequest req, UserPrincipal principal) {
        ResourceRequest request = findById(id);

        // Yetki
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new BusinessRuleException("Gönüllüler talep durumunu güncelleyemez");
        }
        if (role == UserRole.DISTRICT_COORDINATOR
                && !request.getDistrict().getId().equals(principal.getDistrictId())) {
            throw new BusinessRuleException("Sadece kendi ilçenizdeki talepleri yönetebilirsiniz");
        }
        if (role == UserRole.NEIGHBORHOOD_COORDINATOR
                && (request.getNeighborhood() == null
                    || !request.getNeighborhood().getId().equals(principal.getNeighborhoodId()))) {
            throw new BusinessRuleException("Sadece kendi mahallenizin taleplerini yönetebilirsiniz");
        }

        request.setStatus(req.getStatus());
        if (req.getStatus() == ResourceRequestStatus.FULFILLED
                || req.getStatus() == ResourceRequestStatus.CANCELLED) {
            request.setResolvedAt(OffsetDateTime.now());
            request.setClosedAt(OffsetDateTime.now());
            User closer = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
            request.setClosedBy(closer);
        }

        ResourceRequestResponse updated = toResponse(resourceRequestRepository.save(request));
        eventPublisher.publishEvent(new ResourceRequestStatusChangedEmailEvent(id, req.getStatus().name()));
        return updated;
    }

    @Transactional(readOnly = true)
    public List<ResourceSummaryResponse> getResourceSummary() {
        List<Object[]> rows = resourceRequestRepository.findOpenRequestCountByNeighborhood();
        return rows.stream().map(r -> ResourceSummaryResponse.builder()
                .neighborhoodId((UUID) r[0])
                .openCount(((Number) r[1]).intValue())
                .districtId((UUID) r[2])
                .build()).toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceRequestResponse> getOpenByNeighborhood(UUID neighborhoodId) {
        return resourceRequestRepository
                .findByNeighborhoodIdAndStatus(neighborhoodId, ResourceRequestStatus.OPEN)
                .stream().map(this::toResponse).toList();
    }

    private ResourceRequest findById(UUID id) {
        return resourceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResourceRequest", "id", id));
    }

    private ResourceRequestResponse toResponse(ResourceRequest r) {
        return ResourceRequestResponse.builder()
                .id(r.getId())
                .districtId(r.getDistrict().getId())
                .districtName(r.getDistrict().getName())
                .neighborhoodId(r.getNeighborhood() != null ? r.getNeighborhood().getId() : null)
                .neighborhoodName(r.getNeighborhood() != null ? r.getNeighborhood().getName() : null)
                .requestScope(r.getRequestScope().name())
                .resourceType(r.getResourceType().name())
                .resourceTypeLabel(r.getResourceType().getLabel())
                .quantity(r.getQuantity())
                .status(r.getStatus().name())
                .statusLabel(r.getStatus().getLabel())
                .description(r.getDescription())
                .createdBy(r.getCreatedBy() != null
                        ? r.getCreatedBy().getFirstName() + " " + r.getCreatedBy().getLastName() : null)
                .closedBy(r.getClosedBy() != null
                        ? r.getClosedBy().getFirstName() + " " + r.getClosedBy().getLastName() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .closedAt(r.getClosedAt())
                .build();
    }
}
