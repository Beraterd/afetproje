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
import org.springframework.data.jpa.domain.Specification;
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
            ResourceType resourceType, String status, String priority, String search,
            UserPrincipal principal) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Koordinatör kapsamı: yalnızca kendi ilçe/mahallesi
        if (principal.getRole() == UserRole.DISTRICT_COORDINATOR) {
            districtId = principal.getDistrictId();
        }
        if (principal.getRole() == UserRole.NEIGHBORHOOD_COORDINATOR) {
            districtId = principal.getDistrictId();
            neighborhoodId = principal.getNeighborhoodId();
        }

        ResourceRequestStatus statusEnum = parseEnum(ResourceRequestStatus.class, status);
        RequestPriority priorityEnum = parseEnum(RequestPriority.class, priority);
        String pattern = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%" : null;

        final UUID fDistrictId = districtId;
        final UUID fNeighborhoodId = neighborhoodId;

        Specification<ResourceRequest> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            if (fDistrictId != null) ps.add(cb.equal(root.get("district").get("id"), fDistrictId));
            if (fNeighborhoodId != null) ps.add(cb.equal(root.get("neighborhood").get("id"), fNeighborhoodId));
            if (resourceType != null) ps.add(cb.equal(root.get("resourceType"), resourceType));
            if (statusEnum != null) ps.add(cb.equal(root.get("status"), statusEnum));
            if (priorityEnum != null) ps.add(cb.equal(root.get("priority"), priorityEnum));
            if (pattern != null) {
                ps.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("title"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return PagedResponse.from(resourceRequestRepository.findAll(spec, pageable).map(this::toResponse));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Geçersiz değer: " + value);
        }
    }

    @Transactional(readOnly = true)
    public ResourceRequestResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    /** Ürün adının zorunlu olduğu kategoriler (stok formuyla aynı mantık). */
    private static final java.util.Set<ResourceType> NAME_REQUIRED = java.util.EnumSet.of(
            ResourceType.FOOD, ResourceType.HYGIENE, ResourceType.MEDICAL_SUPPORT,
            ResourceType.HEAVY_MACHINERY, ResourceType.OTHER);

    /**
     * Kategoriye göre ürün adını çözer: ad zorunlu kategorilerde boşsa hata,
     * diğerlerinde kategori label'ı kullanılır.
     */
    private String resolveRequestName(ResourceType category, String name) {
        boolean provided = name != null && !name.isBlank();
        if (NAME_REQUIRED.contains(category)) {
            if (!provided) {
                throw new BusinessRuleException("Bu kategori için ürün adı zorunludur");
            }
            return name.trim();
        }
        return provided ? name.trim() : category.getLabel();
    }

    @Transactional
    public ResourceRequestResponse create(CreateResourceRequestRequest req, UserPrincipal principal) {
        UserRole role = principal.getRole();
        if (role == UserRole.VOLUNTEER) {
            throw new BusinessRuleException("Gönüllüler kaynak talebi oluşturamaz");
        }
        // İlçe + mahalle bölgesel takip için zorunlu (DTO doğrulamasına ek servis güvencesi).
        if (req.getNeighborhoodId() == null) {
            throw new BusinessRuleException("Mahalle seçimi zorunludur");
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
                .title(resolveRequestName(req.getResourceType(), req.getName()))
                .quantity(req.getQuantity())
                .unit(req.getUnit())
                .priority(req.getPriority() != null ? req.getPriority() : RequestPriority.MEDIUM)
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
                .productName(r.getTitle() != null && !r.getTitle().isBlank()
                        ? r.getTitle() : r.getResourceType().getLabel())
                .quantity(r.getQuantity())
                .unit(r.getUnit())
                .priority(r.getPriority() != null ? r.getPriority().name() : null)
                .priorityLabel(r.getPriority() != null ? r.getPriority().getLabel() : null)
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
