package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.Street;
import com.afet.koordinasyon.dto.response.StreetBounds;
import com.afet.koordinasyon.dto.response.StreetCenter;
import com.afet.koordinasyon.dto.response.StreetDetailResponse;
import com.afet.koordinasyon.dto.response.StreetSearchResultResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.StreetRepository;
import com.afet.koordinasyon.util.TurkishTextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Mahalle-scoped sokak/cadde autocomplete arama (item 8-13). Building search'ten (bkz.
 * BuildingService.search) kasıtlı olarak AYRI bir servis/endpoint — bina adres coverage'ına
 * bağımlı olmaması gerekiyor (item 1) ve neighborhoodId zorunlu olduğu için farklı bir sözleşme.
 */
@Service
@RequiredArgsConstructor
public class StreetService {

    private static final int DEFAULT_SEARCH_LIMIT = 10;
    private static final int MAX_SEARCH_LIMIT = 20;
    /** Sokak isimleri bina adreslerinden daha kısa anlamlı önek taşıyabilir (item 14) —
     *  building search'ün 3 karakter eşiğinden bilinçli olarak farklı. */
    private static final int MIN_SEARCH_QUERY_LENGTH = 2;

    private final StreetRepository streetRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<StreetSearchResultResponse> search(String q, UUID districtId, UUID neighborhoodId, Integer limit) {
        if (q == null || q.trim().length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new BusinessRuleException("Arama metni en az " + MIN_SEARCH_QUERY_LENGTH + " karakter olmalıdır");
        }
        String normalizedQ = TurkishTextNormalizer.normalize(q);
        if (normalizedQ.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new BusinessRuleException("Arama metni en az " + MIN_SEARCH_QUERY_LENGTH + " karakter olmalıdır");
        }

        Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId)
                .orElseThrow(() -> new ResourceNotFoundException("Neighborhood", "id", neighborhoodId));
        if (!neighborhood.getDistrict().getId().equals(districtId)) {
            throw new BusinessRuleException("Bu mahalle belirtilen ilçeye ait değil");
        }

        int effectiveLimit = Math.min(limit != null ? limit : DEFAULT_SEARCH_LIMIT, MAX_SEARCH_LIMIT);
        return streetRepository.search(districtId, neighborhoodId, normalizedQ, PageRequest.of(0, effectiveLimit)).stream()
                .map(this::toSearchResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public StreetDetailResponse getById(UUID id) {
        Street street = streetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Street", "id", id));
        return StreetDetailResponse.builder()
                .id(street.getId())
                .name(street.getName())
                .districtId(street.getDistrict().getId())
                .districtName(street.getDistrict().getName())
                .neighborhoodId(street.getNeighborhood().getId())
                .neighborhoodName(street.getNeighborhood().getName())
                .roadType(street.getRoadType())
                .geometry(parseGeometry(street.getGeometry()))
                .bounds(toBounds(street))
                .center(toCenter(street))
                .build();
    }

    private StreetSearchResultResponse toSearchResult(Street street) {
        return StreetSearchResultResponse.builder()
                .id(street.getId())
                .name(street.getName())
                .districtId(street.getDistrict().getId())
                .districtName(street.getDistrict().getName())
                .neighborhoodId(street.getNeighborhood().getId())
                .neighborhoodName(street.getNeighborhood().getName())
                .roadType(street.getRoadType())
                .bounds(toBounds(street))
                .center(toCenter(street))
                .build();
    }

    private StreetBounds toBounds(Street s) {
        return StreetBounds.builder()
                .minLon(s.getBboxMinLon().doubleValue())
                .minLat(s.getBboxMinLat().doubleValue())
                .maxLon(s.getBboxMaxLon().doubleValue())
                .maxLat(s.getBboxMaxLat().doubleValue())
                .build();
    }

    private StreetCenter toCenter(Street s) {
        return StreetCenter.builder()
                .lon(s.getCenterLon().doubleValue())
                .lat(s.getCenterLat().doubleValue())
                .build();
    }

    private JsonNode parseGeometry(String geometryJson) {
        try {
            return objectMapper.readTree(geometryJson);
        } catch (Exception e) {
            throw new IllegalStateException("Bozuk sokak geometrisi (DB tutarsızlığı)", e);
        }
    }
}
