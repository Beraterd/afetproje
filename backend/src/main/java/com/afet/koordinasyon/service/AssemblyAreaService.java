package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AssemblyArea;
import com.afet.koordinasyon.dto.request.BulkReviewRequest;
import com.afet.koordinasyon.dto.request.ReviewAssemblyAreaRequest;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.dto.response.AssemblyAreaResponse;
import com.afet.koordinasyon.dto.response.AssemblyAreaStatsResponse;
import com.afet.koordinasyon.dto.response.AssemblyCoverageResponse;
import com.afet.koordinasyon.dto.response.DistrictSummaryResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodSummaryResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.AssemblyAreaRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssemblyAreaService {

    private final AssemblyAreaRepository assemblyAreaRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final UserRepository userRepository;

    /**
     * Giriş yapmış kullanıcının mahallesindeki aktif/onaylı toplanma alanlarını döner.
     * Kullanıcının mahallesi yoksa boş liste döner.
     */
    @Transactional(readOnly = true)
    public List<AssemblyAreaResponse> getMyNeighborhoodAreas(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (user.getNeighborhood() == null) {
            return List.of();
        }
        return assemblyAreaRepository
                .findActiveApprovedByNeighborhoodId(user.getNeighborhood().getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AssemblyAreaResponse> listWithFilters(
            UUID districtId, UUID neighborhoodId, String sourceName,
            Boolean needsReview, Boolean active, String search, int page, int size) {

        // Hibernate 6'da null String parametresi LOWER/CONCAT içinde bytea bind edilip
        // "lower(bytea)" hatası verdiğinden, arama için NON-NULL pattern + boolean flag kullanılır.
        boolean hasSearch = search != null && !search.isBlank();
        String pattern = hasSearch ? "%" + search.trim().toLowerCase() + "%" : "%";

        Page<AssemblyArea> result = assemblyAreaRepository.findWithFilters(
                districtId, neighborhoodId, sourceName, needsReview, active, hasSearch, pattern,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));

        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AssemblyAreaResponse getById(UUID id) {
        AssemblyArea area = assemblyAreaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssemblyArea", "id", id));
        return toResponse(area);
    }

    @Transactional
    public AssemblyAreaResponse update(UUID id, ReviewAssemblyAreaRequest request) {
        AssemblyArea area = assemblyAreaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AssemblyArea", "id", id));

        if (request.getName() != null) area.setName(request.getName());
        if (request.getAddress() != null) area.setAddress(request.getAddress());
        if (request.getLatitude() != null) area.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) area.setLongitude(request.getLongitude());
        if (request.getNeedsReview() != null) area.setNeedsReview(request.getNeedsReview());
        if (request.getActive() != null) area.setActive(request.getActive());

        // google_maps_url: use explicit value or auto-generate from coordinates
        if (request.getGoogleMapsUrl() != null && !request.getGoogleMapsUrl().isBlank()) {
            area.setGoogleMapsUrl(request.getGoogleMapsUrl());
        } else if (area.getLatitude() != null && area.getLongitude() != null) {
            area.setGoogleMapsUrl(buildMapsUrl(area.getLatitude(), area.getLongitude()));
        }

        AssemblyArea saved = assemblyAreaRepository.save(area);
        log.info("AssemblyArea {} updated: needsReview={}, hasCoords={}",
                saved.getId(), saved.isNeedsReview(),
                saved.getLatitude() != null && saved.getLongitude() != null);
        return toResponse(saved);
    }

    @Transactional
    public int bulkUpdate(BulkReviewRequest request) {
        List<AssemblyArea> areas = assemblyAreaRepository.findAllById(request.getIds());
        for (AssemblyArea area : areas) {
            if (request.getNeedsReview() != null) area.setNeedsReview(request.getNeedsReview());
            if (request.getActive() != null) area.setActive(request.getActive());
        }
        assemblyAreaRepository.saveAll(areas);
        return areas.size();
    }

    @Transactional(readOnly = true)
    public int countPendingReview() {
        return assemblyAreaRepository.countByNeedsReviewTrue();
    }

    @Transactional(readOnly = true)
    public AssemblyAreaStatsResponse getStats() {
        return AssemblyAreaStatsResponse.builder()
                .pendingReview(assemblyAreaRepository.countByNeedsReviewTrue())
                .total(assemblyAreaRepository.countTotal())
                .build();
    }

    /**
     * İlçe/mahalle bazında toplanma alanı kapsama raporu üretir.
     * Hangi mahallelerin verified assembly area'sı var/yok gösterir.
     */
    @Transactional(readOnly = true)
    public AssemblyCoverageResponse getCoverage() {
        Set<UUID> covered = Set.copyOf(assemblyAreaRepository.findCoveredNeighborhoodIds());

        // district.id → list of neighborhoods (JOIN FETCH'd)
        Map<UUID, List<Neighborhood>> byDistrict = neighborhoodRepository.findAllWithDistrict()
                .stream()
                .collect(Collectors.groupingBy(
                        n -> n.getDistrict().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<AssemblyCoverageResponse.DistrictCoverage> districtEntries = new ArrayList<>();
        int totalNeighborhoods = 0;
        int totalCovered = 0;

        for (Map.Entry<UUID, List<Neighborhood>> entry : byDistrict.entrySet()) {
            List<Neighborhood> neighborhoods = entry.getValue();
            String districtName = neighborhoods.get(0).getDistrict().getName();

            List<String> uncoveredNames = neighborhoods.stream()
                    .filter(n -> !covered.contains(n.getId()))
                    .map(Neighborhood::getName)
                    .sorted()
                    .toList();

            int districtTotal = neighborhoods.size();
            int districtCovered = districtTotal - uncoveredNames.size();
            double pct = districtTotal > 0 ? (districtCovered * 100.0 / districtTotal) : 0.0;

            districtEntries.add(AssemblyCoverageResponse.DistrictCoverage.builder()
                    .districtId(entry.getKey())
                    .districtName(districtName)
                    .totalNeighborhoods(districtTotal)
                    .coveredNeighborhoods(districtCovered)
                    .coveragePct(Math.round(pct * 10.0) / 10.0)
                    .uncoveredNeighborhoodNames(uncoveredNames)
                    .build());

            totalNeighborhoods += districtTotal;
            totalCovered += districtCovered;
        }

        // İlçeleri kapsama yüzdesine göre sırala (en yüksek önce)
        districtEntries.sort((a, b) -> Double.compare(b.getCoveragePct(), a.getCoveragePct()));

        int uncovered = totalNeighborhoods - totalCovered;
        double overallPct = totalNeighborhoods > 0 ? (totalCovered * 100.0 / totalNeighborhoods) : 0.0;

        return AssemblyCoverageResponse.builder()
                .totalNeighborhoods(totalNeighborhoods)
                .coveredNeighborhoods(totalCovered)
                .uncoveredNeighborhoods(uncovered)
                .coveragePct(Math.round(overallPct * 10.0) / 10.0)
                .districts(districtEntries)
                .build();
    }

    private String buildMapsUrl(BigDecimal lat, BigDecimal lng) {
        return "https://www.google.com/maps?q=" + lat + "," + lng;
    }

    private AssemblyAreaResponse toResponse(AssemblyArea a) {
        return AssemblyAreaResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .address(a.getAddress())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .googleMapsUrl(a.getGoogleMapsUrl())
                .capacity(a.getCapacity())
                .description(a.getDescription())
                .sourceName(a.getSourceName())
                .sourceReference(a.getSourceReference())
                .needsReview(a.isNeedsReview())
                .active(a.isActive())
                .createdAt(a.getCreatedAt())
                .district(a.getDistrict() != null
                        ? DistrictSummaryResponse.builder()
                                .id(a.getDistrict().getId())
                                .name(a.getDistrict().getName())
                                .build()
                        : null)
                .neighborhood(a.getNeighborhood() != null
                        ? NeighborhoodSummaryResponse.builder()
                                .id(a.getNeighborhood().getId())
                                .name(a.getNeighborhood().getName())
                                .districtId(a.getNeighborhood().getDistrict() != null
                                        ? a.getNeighborhood().getDistrict().getId() : null)
                                .districtName(a.getNeighborhood().getDistrict() != null
                                        ? a.getNeighborhood().getDistrict().getName() : null)
                                .build()
                        : null)
                .build();
    }
}
