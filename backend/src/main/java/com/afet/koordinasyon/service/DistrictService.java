package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.dto.response.DistrictResponse;
import com.afet.koordinasyon.dto.response.NeighborhoodSummaryResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistrictService {

    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final RiskCalculationService riskCalculationService;

    @Transactional(readOnly = true)
    public List<DistrictResponse> listActive() {
        return districtRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DistrictResponse getById(UUID id) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", id));
        return toResponse(district);
    }

    @Transactional(readOnly = true)
    public List<NeighborhoodSummaryResponse> listNeighborhoods(UUID districtId) {
        districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        return neighborhoodRepository.findByDistrictId(districtId).stream()
                .map(n -> NeighborhoodSummaryResponse.builder()
                        .id(n.getId())
                        .name(n.getName())
                        .districtId(districtId)
                        .districtName(n.getDistrict().getName())
                        .build())
                .toList();
    }

    private DistrictResponse toResponse(District d) {
        return DistrictResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .riskScore(d.getRiskScore())
                .riskColor(riskCalculationService.getRiskColor(d.getRiskScore()))
                .active(d.isActive())
                .riskScoreUpdatedAt(d.getRiskScoreUpdatedAt())
                .build();
    }
}
