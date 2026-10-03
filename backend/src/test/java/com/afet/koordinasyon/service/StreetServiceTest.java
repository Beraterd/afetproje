package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.Street;
import com.afet.koordinasyon.dto.response.StreetSearchResultResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.StreetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreetServiceTest {

    @Mock private StreetRepository streetRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;

    private StreetService service;

    private District kartal;
    private District kadikoy;
    private Neighborhood atalar;

    @BeforeEach
    void setUp() {
        service = new StreetService(streetRepository, neighborhoodRepository);
        kartal = District.builder().id(UUID.randomUUID()).name("Kartal").build();
        kadikoy = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        atalar = Neighborhood.builder().id(UUID.randomUUID()).name("Atalar").district(kartal).build();
    }

    private Street street(Neighborhood n, String name) {
        return Street.builder()
                .id(UUID.randomUUID())
                .source("OPENSTREETMAP")
                .externalId("way/1")
                .district(n.getDistrict())
                .neighborhood(n)
                .name(name)
                .normalizedName(name.toLowerCase())
                .roadType("residential")
                .geometry("{\"type\":\"MultiLineString\",\"coordinates\":[[[29.18,40.9],[29.181,40.901]]]}")
                .bboxMinLat(BigDecimal.valueOf(40.9)).bboxMaxLat(BigDecimal.valueOf(40.901))
                .bboxMinLon(BigDecimal.valueOf(29.18)).bboxMaxLon(BigDecimal.valueOf(29.181))
                .centerLat(BigDecimal.valueOf(40.9005)).centerLon(BigDecimal.valueOf(29.1805))
                .build();
    }

    // ── search ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("1 karakterlik arama reddedilir (min 2 karakter)")
    void search_tooShort_throws() {
        assertThatThrownBy(() -> service.search("a", kartal.getId(), atalar.getId(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Olmayan mahalle 404 (ResourceNotFoundException)")
    void search_neighborhoodNotFound_throws() {
        when(neighborhoodRepository.findById(atalar.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search("selvi", kartal.getId(), atalar.getId(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Mahalle başka bir ilçeye aitse reddedilir (districtId/neighborhoodId tutarsızlığı)")
    void search_neighborhoodBelongsToDifferentDistrict_throws() {
        when(neighborhoodRepository.findById(atalar.getId())).thenReturn(Optional.of(atalar));

        assertThatThrownBy(() -> service.search("selvi", kadikoy.getId(), atalar.getId(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Geçerli arama sonuçları doğru DTO alanlarıyla döner")
    void search_returnsResults() {
        Street s = street(atalar, "Selvi Boylum Sokak");
        when(neighborhoodRepository.findById(atalar.getId())).thenReturn(Optional.of(atalar));
        when(streetRepository.search(eq(kartal.getId()), eq(atalar.getId()), eq("selvi"), any(Pageable.class)))
                .thenReturn(List.of(s));

        List<StreetSearchResultResponse> results = service.search("Selvi", kartal.getId(), atalar.getId(), null);

        assertThat(results).hasSize(1);
        StreetSearchResultResponse r = results.get(0);
        assertThat(r.getName()).isEqualTo("Selvi Boylum Sokak");
        assertThat(r.getNeighborhoodId()).isEqualTo(atalar.getId());
        assertThat(r.getDistrictId()).isEqualTo(kartal.getId());
        assertThat(r.getBounds().getMinLat()).isEqualTo(40.9);
        assertThat(r.getCenter().getLat()).isEqualTo(40.9005);
    }

    @Test
    @DisplayName("Türkçe katlama: arama sorgusu normalize edilerek repository'ye geçirilir")
    void search_normalizesTurkishQuery() {
        when(neighborhoodRepository.findById(atalar.getId())).thenReturn(Optional.of(atalar));
        when(streetRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(List.of());

        service.search("Şişli Çeşme", kartal.getId(), atalar.getId(), null);

        verify(streetRepository).search(eq(kartal.getId()), eq(atalar.getId()), eq("sisli cesme"), any(Pageable.class));
    }

    @Test
    @DisplayName("limit MAX_SEARCH_LIMIT'i aşamaz")
    void search_limitIsCapped() {
        when(neighborhoodRepository.findById(atalar.getId())).thenReturn(Optional.of(atalar));
        when(streetRepository.search(any(), any(), any(), any(Pageable.class))).thenReturn(List.of());

        service.search("selvi", kartal.getId(), atalar.getId(), 999);

        var captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(streetRepository).search(eq(kartal.getId()), eq(atalar.getId()), eq("selvi"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isLessThanOrEqualTo(20);
    }

    // ── getById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Olmayan streetId 404 (ResourceNotFoundException)")
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(streetRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Sokak detayı geometry/bounds/center ile döner")
    void getById_returnsDetailWithGeometry() {
        Street s = street(atalar, "Selvi Boylum Sokak");
        when(streetRepository.findById(s.getId())).thenReturn(Optional.of(s));

        var detail = service.getById(s.getId());

        assertThat(detail.getName()).isEqualTo("Selvi Boylum Sokak");
        assertThat(detail.getGeometry().get("type").asText()).isEqualTo("MultiLineString");
        assertThat(detail.getBounds().getMaxLon()).isEqualTo(29.181);
    }
}
