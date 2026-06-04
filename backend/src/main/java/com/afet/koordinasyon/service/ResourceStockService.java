package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.*;
import com.afet.koordinasyon.domain.enums.ResourceStockMovementType;
import com.afet.koordinasyon.domain.enums.ResourceStockStatus;
import com.afet.koordinasyon.domain.enums.ResourceType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.CreateResourceStockRequest;
import com.afet.koordinasyon.dto.request.UpdateResourceStockRequest;
import com.afet.koordinasyon.dto.request.UpdateStockQuantityRequest;
import com.afet.koordinasyon.dto.response.ResourceStockMovementResponse;
import com.afet.koordinasyon.dto.response.ResourceStockResponse;
import com.afet.koordinasyon.dto.response.ResourceStockSummaryResponse;
import com.afet.koordinasyon.dto.response.StockLookupResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceStockMovementRepository;
import com.afet.koordinasyon.repository.ResourceStockRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceStockService {

    private final ResourceStockRepository stockRepository;
    private final ResourceStockMovementRepository movementRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;

    /** Ürün adının zorunlu olduğu kategoriler. Diğerlerinde isim kategori label'ından türetilir. */
    private static final java.util.Set<ResourceType> NAME_REQUIRED = java.util.EnumSet.of(
            ResourceType.FOOD, ResourceType.HYGIENE, ResourceType.MEDICAL_SUPPORT,
            ResourceType.HEAVY_MACHINERY, ResourceType.OTHER);

    /**
     * Kategoriye göre ürün adını çözer:
     * <ul>
     *   <li>OTHER: ad veya not'tan en az biri zorunlu; ad boşsa label kullanılır.</li>
     *   <li>Ad zorunlu kategoriler (FOOD/HYGIENE/MEDICAL_SUPPORT/HEAVY_MACHINERY): ad boşsa hata.</li>
     *   <li>Diğerleri (WATER/TENT/BLANKET/GENERATOR/HEATING): ad boşsa kategori label'ı kullanılır.</li>
     * </ul>
     */
    private String resolveStockName(ResourceType category, String name, String notes) {
        boolean nameProvided = name != null && !name.isBlank();
        if (category == ResourceType.OTHER) {
            boolean notesProvided = notes != null && !notes.isBlank();
            if (!nameProvided && !notesProvided) {
                throw new BusinessRuleException("\"Diğer\" kategorisinde ürün adı veya açıklama zorunludur");
            }
            return nameProvided ? name.trim() : category.getLabel();
        }
        if (NAME_REQUIRED.contains(category)) {
            if (!nameProvided) {
                throw new BusinessRuleException("Bu kategori için ürün adı zorunludur");
            }
            return name.trim();
        }
        return nameProvided ? name.trim() : category.getLabel();
    }

    // ── Durum / yeterlilik hesabı ────────────────────────────────────────────

    /** dailyUsageEstimate yoksa veya <=0 ise null (yeterlilik bilinmiyor). */
    public Double computeDaysRemaining(int quantity, Double dailyUsageEstimate) {
        if (dailyUsageEstimate == null || dailyUsageEstimate <= 0) {
            return null;
        }
        return Math.round((quantity / dailyUsageEstimate) * 10.0) / 10.0;
    }

    public ResourceStockStatus computeStatus(int quantity, int criticalThreshold, Double dailyUsageEstimate) {
        if (quantity <= 0) {
            return ResourceStockStatus.OUT_OF_STOCK;
        }
        if (quantity <= criticalThreshold) {
            return ResourceStockStatus.CRITICAL;
        }
        Double days = computeDaysRemaining(quantity, dailyUsageEstimate);
        if (days != null) {
            if (days <= 2) return ResourceStockStatus.CRITICAL;
            if (days <= 5) return ResourceStockStatus.DECREASING;
        }
        return ResourceStockStatus.SUFFICIENT;
    }

    // ── Listeleme / özet ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResourceStockResponse> list(UUID districtId, UUID neighborhoodId, ResourceType category,
                                            String status, String search, boolean criticalOnly,
                                            UserPrincipal principal) {
        ScopeFilter scope = resolveScopeFilter(districtId, neighborhoodId, principal);
        // Hibernate 6'da null String parametresi LOWER/CONCAT içinde bytea olarak bind edilip
        // "lower(bytea)" hatasına yol açıyor. Bu yüzden her zaman NON-NULL pattern + boolean flag.
        boolean hasSearch = search != null && !search.isBlank();
        String pattern = hasSearch ? "%" + search.trim().toLowerCase() + "%" : "%";

        return stockRepository.search(scope.districtId(), scope.neighborhoodId(), category, hasSearch, pattern)
                .stream()
                .map(this::toResponse)
                .filter(r -> status == null || status.isBlank() || r.getStatus().equals(status))
                .filter(r -> !criticalOnly
                        || r.getStatus().equals(ResourceStockStatus.CRITICAL.name())
                        || r.getStatus().equals(ResourceStockStatus.OUT_OF_STOCK.name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceStockSummaryResponse summary(UUID districtId, UUID neighborhoodId, UserPrincipal principal) {
        ScopeFilter scope = resolveScopeFilter(districtId, neighborhoodId, principal);
        List<ResourceStock> stocks = stockRepository.search(
                scope.districtId(), scope.neighborhoodId(), null, false, "%");

        int total = stocks.size();
        int critical = 0;
        int outOfStock = 0;
        double daysSum = 0;
        int daysCount = 0;

        for (ResourceStock s : stocks) {
            ResourceStockStatus st = computeStatus(s.getQuantity(), s.getCriticalThreshold(), s.getDailyUsageEstimate());
            if (st == ResourceStockStatus.OUT_OF_STOCK) outOfStock++;
            else if (st == ResourceStockStatus.CRITICAL) critical++;
            Double days = computeDaysRemaining(s.getQuantity(), s.getDailyUsageEstimate());
            if (days != null) {
                daysSum += days;
                daysCount++;
            }
        }

        Double avgDays = daysCount > 0 ? Math.round((daysSum / daysCount) * 10.0) / 10.0 : null;
        return ResourceStockSummaryResponse.builder()
                .totalItems(total)
                .criticalCount(critical)
                .outOfStockCount(outOfStock)
                .averageDaysRemaining(avgDays)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ResourceStockMovementResponse> getMovements(UUID stockId, UserPrincipal principal) {
        ResourceStock stock = findById(stockId);
        assertCanView(principal, stock);
        return movementRepository.findByStockIdOrderByCreatedAtDesc(stockId)
                .stream().map(this::toMovementResponse).toList();
    }

    // ── Oluşturma / güncelleme ────────────────────────────────────────────────

    @Transactional
    public ResourceStockResponse create(CreateResourceStockRequest req, UserPrincipal principal) {
        District district = districtRepository.findById(req.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", req.getDistrictId()));

        if (req.getNeighborhoodId() == null) {
            throw new BusinessRuleException("Mahalle seçimi zorunludur");
        }
        Neighborhood neighborhood = neighborhoodRepository.findById(req.getNeighborhoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", req.getNeighborhoodId()));
        if (!neighborhood.getDistrict().getId().equals(district.getId())) {
            throw new BusinessRuleException("Seçilen mahalle bu ilçeye ait değil");
        }

        assertCanManage(principal, district, neighborhood);

        // Ürün adı kategoriye göre: bazı kategorilerde zorunlu, bazılarında label'dan türetilir.
        String resolvedName = resolveStockName(req.getCategory(), req.getName(), req.getNotes());

        User actor = currentUser(principal);
        int initialQty = req.getQuantity() != null ? req.getQuantity() : 0;

        ResourceStock stock = stockRepository.save(ResourceStock.builder()
                .name(resolvedName)
                .category(req.getCategory())
                .quantity(initialQty)
                .unit(req.getUnit())
                .dailyUsageEstimate(req.getDailyUsageEstimate())
                .criticalThreshold(req.getCriticalThreshold() != null ? req.getCriticalThreshold() : 0)
                .district(district)
                .neighborhood(neighborhood)
                .notes(req.getNotes())
                .updatedBy(actor)
                .build());

        recordMovement(stock, ResourceStockMovementType.INITIAL, initialQty, 0, initialQty,
                "İlk stok kaydı", actor);

        return toResponse(stock);
    }

    @Transactional
    public ResourceStockResponse update(UUID id, UpdateResourceStockRequest req, UserPrincipal principal) {
        ResourceStock stock = findById(id);
        assertCanManage(principal, stock.getDistrict(), stock.getNeighborhood());

        if (req.getName() != null && !req.getName().isBlank()) stock.setName(req.getName());
        if (req.getUnit() != null && !req.getUnit().isBlank()) stock.setUnit(req.getUnit());
        if (req.getDailyUsageEstimate() != null) stock.setDailyUsageEstimate(req.getDailyUsageEstimate());
        if (req.getCriticalThreshold() != null) stock.setCriticalThreshold(req.getCriticalThreshold());
        if (req.getWarehouseName() != null) stock.setWarehouseName(req.getWarehouseName());
        if (req.getNotes() != null) stock.setNotes(req.getNotes());
        stock.setUpdatedBy(currentUser(principal));

        return toResponse(stockRepository.save(stock));
    }

    @Transactional
    public ResourceStockResponse updateQuantity(UUID id, UpdateStockQuantityRequest req, UserPrincipal principal) {
        ResourceStock stock = findById(id);
        assertCanManage(principal, stock.getDistrict(), stock.getNeighborhood());

        int previous = stock.getQuantity();
        int next = req.getNewQuantity();
        int change = next - previous;

        ResourceStockMovementType type = req.getMovementType();
        if (type == null) {
            type = change > 0 ? ResourceStockMovementType.INCREASE
                    : change < 0 ? ResourceStockMovementType.DECREASE
                    : ResourceStockMovementType.CORRECTION;
        }

        User actor = currentUser(principal);
        stock.setQuantity(next);
        stock.setUpdatedBy(actor);
        stockRepository.save(stock);

        recordMovement(stock, type, change, previous, next, req.getReason(), actor);
        return toResponse(stock);
    }

    @Transactional
    public void deactivate(UUID id, UserPrincipal principal) {
        ResourceStock stock = findById(id);
        assertCanManage(principal, stock.getDistrict(), stock.getNeighborhood());
        stock.setActive(false);
        stock.setUpdatedBy(currentUser(principal));
        stockRepository.save(stock);
    }

    // ── Talep uyarısı (lookup) ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StockLookupResponse lookup(UUID districtId, UUID neighborhoodId, ResourceType category,
                                      Integer requestedQuantity) {
        List<ResourceStock> stocks = stockRepository.findForLookup(districtId, neighborhoodId, category);
        if (stocks.isEmpty()) {
            return StockLookupResponse.builder()
                    .hasStock(false)
                    .totalQuantity(0)
                    .message("Bu bölgede bu türde kayıtlı stok bulunamadı. Talep açabilirsiniz.")
                    .build();
        }

        int totalQuantity = stocks.stream().mapToInt(ResourceStock::getQuantity).sum();
        int totalCritical = stocks.stream().mapToInt(ResourceStock::getCriticalThreshold).sum();
        double totalDaily = stocks.stream()
                .filter(s -> s.getDailyUsageEstimate() != null && s.getDailyUsageEstimate() > 0)
                .mapToDouble(ResourceStock::getDailyUsageEstimate).sum();
        Double daysRemaining = totalDaily > 0
                ? Math.round((totalQuantity / totalDaily) * 10.0) / 10.0 : null;
        String unit = stocks.get(0).getUnit();
        ResourceStockStatus status = computeStatus(totalQuantity, totalCritical,
                totalDaily > 0 ? totalDaily : null);

        StringBuilder msg = new StringBuilder();
        msg.append("Bu bölgede mevcut stok: ").append(totalQuantity).append(" ").append(unit);
        if (daysRemaining != null) {
            msg.append(". Tahmini yeterlilik: ").append(daysRemaining).append(" gün");
        }
        msg.append(".");

        if (status == ResourceStockStatus.OUT_OF_STOCK || status == ResourceStockStatus.CRITICAL) {
            msg.append(" Bu bölgede stok kritik seviyede. Talep açılması önerilir.");
        } else if (requestedQuantity != null && requestedQuantity > totalQuantity) {
            msg.append(" Talep edilen miktar mevcut stoktan fazla.");
        } else {
            msg.append(" Bu bölgede stok yeterli görünüyor. Yine de talep açabilirsiniz.");
        }

        return StockLookupResponse.builder()
                .hasStock(true)
                .totalQuantity(totalQuantity)
                .unit(unit)
                .status(status.name())
                .statusLabel(status.getLabel())
                .daysRemaining(daysRemaining)
                .message(msg.toString())
                .build();
    }

    // ── Yetki yardımcıları ─────────────────────────────────────────────────────

    /** Koordinatörlerin liste/özet sorgusunu kendi yetki alanına daraltır. */
    private ScopeFilter resolveScopeFilter(UUID districtId, UUID neighborhoodId, UserPrincipal principal) {
        UserRole role = principal.getRole();
        if (role == UserRole.DISTRICT_COORDINATOR) {
            return new ScopeFilter(principal.getDistrictId(), neighborhoodId);
        }
        if (role == UserRole.NEIGHBORHOOD_COORDINATOR) {
            return new ScopeFilter(principal.getDistrictId(), principal.getNeighborhoodId());
        }
        // ADMIN ve diğerleri: istenen filtreler uygulanır
        return new ScopeFilter(districtId, neighborhoodId);
    }

    /** Yazma yetkisi: admin her yer, koordinatör yalnızca kendi ilçe/mahallesi, gönüllü hiç. */
    private void assertCanManage(UserPrincipal principal, District district, Neighborhood neighborhood) {
        UserRole role = principal.getRole();
        switch (role) {
            case ADMIN -> { /* tüm bölgeler */ }
            case DISTRICT_COORDINATOR -> {
                if (district == null || !district.getId().equals(principal.getDistrictId())) {
                    throw new AccessDeniedException("Sadece kendi ilçenizdeki stokları yönetebilirsiniz");
                }
            }
            case NEIGHBORHOOD_COORDINATOR -> {
                if (neighborhood == null || !neighborhood.getId().equals(principal.getNeighborhoodId())) {
                    throw new AccessDeniedException("Sadece kendi mahallenizdeki stokları yönetebilirsiniz");
                }
            }
            default -> throw new AccessDeniedException("Stok yönetme yetkiniz yok");
        }
    }

    /** Okuma yetkisi: koordinatör yalnızca kendi alanını görür; admin/gönüllü görebilir. */
    private void assertCanView(UserPrincipal principal, ResourceStock stock) {
        UserRole role = principal.getRole();
        if (role == UserRole.DISTRICT_COORDINATOR
                && !stock.getDistrict().getId().equals(principal.getDistrictId())) {
            throw new AccessDeniedException("Bu stok yetki alanınız dışında");
        }
        if (role == UserRole.NEIGHBORHOOD_COORDINATOR
                && (stock.getNeighborhood() == null
                    || !stock.getNeighborhood().getId().equals(principal.getNeighborhoodId()))) {
            throw new AccessDeniedException("Bu stok yetki alanınız dışında");
        }
    }

    // ── İç yardımcılar ──────────────────────────────────────────────────────────

    private void recordMovement(ResourceStock stock, ResourceStockMovementType type,
                                int change, int previous, int next, String reason, User actor) {
        movementRepository.save(ResourceStockMovement.builder()
                .stock(stock)
                .movementType(type)
                .quantityChange(change)
                .previousQuantity(previous)
                .newQuantity(next)
                .reason(reason)
                .createdBy(actor)
                .build());
    }

    private User currentUser(UserPrincipal principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }

    private ResourceStock findById(UUID id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResourceStock", "id", id));
    }

    private ResourceStockResponse toResponse(ResourceStock s) {
        ResourceStockStatus status = computeStatus(s.getQuantity(), s.getCriticalThreshold(), s.getDailyUsageEstimate());
        return ResourceStockResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .category(s.getCategory().name())
                .categoryLabel(s.getCategory().getLabel())
                .quantity(s.getQuantity())
                .unit(s.getUnit())
                .dailyUsageEstimate(s.getDailyUsageEstimate())
                .criticalThreshold(s.getCriticalThreshold())
                .daysRemaining(computeDaysRemaining(s.getQuantity(), s.getDailyUsageEstimate()))
                .status(status.name())
                .statusLabel(status.getLabel())
                .districtId(s.getDistrict().getId())
                .districtName(s.getDistrict().getName())
                .neighborhoodId(s.getNeighborhood() != null ? s.getNeighborhood().getId() : null)
                .neighborhoodName(s.getNeighborhood() != null ? s.getNeighborhood().getName() : null)
                .warehouseName(s.getWarehouseName())
                .notes(s.getNotes())
                .active(s.isActive())
                .updatedBy(s.getUpdatedBy() != null
                        ? s.getUpdatedBy().getFirstName() + " " + s.getUpdatedBy().getLastName() : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ResourceStockMovementResponse toMovementResponse(ResourceStockMovement m) {
        return ResourceStockMovementResponse.builder()
                .id(m.getId())
                .movementType(m.getMovementType().name())
                .movementTypeLabel(m.getMovementType().getLabel())
                .quantityChange(m.getQuantityChange())
                .previousQuantity(m.getPreviousQuantity())
                .newQuantity(m.getNewQuantity())
                .reason(m.getReason())
                .createdBy(m.getCreatedBy() != null
                        ? m.getCreatedBy().getFirstName() + " " + m.getCreatedBy().getLastName() : null)
                .createdAt(m.getCreatedAt())
                .build();
    }

    /** Çözümlenmiş kapsam filtresi (ilçe/mahalle). */
    private record ScopeFilter(UUID districtId, UUID neighborhoodId) {}
}
