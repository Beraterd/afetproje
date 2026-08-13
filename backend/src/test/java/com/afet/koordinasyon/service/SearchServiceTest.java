package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.response.GlobalSearchResponse;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EventRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import com.afet.koordinasyon.repository.TeamRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Global search'ün role/scope davranışını kapsar: VOLUNTEER hiçbir zaman USER sonucu almaz,
 * DISTRICT_COORDINATOR yalnızca kendi ilçesiyle sınırlı arama yapar, ADMIN sınırsız arar,
 * ve kısa (< 2 karakter) sorgular repository'lere hiç dokunmadan boş sonuç döner.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private DamageAssessmentRepository damageAssessmentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResourceRequestRepository resourceRequestRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;

    @InjectMocks private SearchService service;

    private UserPrincipal principal(UserRole role, UUID districtId, UUID neighborhoodId) {
        return new UserPrincipal(UUID.randomUUID(), "Test", "User", "t@x.com",
                null, role, districtId, neighborhoodId, true, false, List.of());
    }

    @BeforeEach
    void setUp() {
        lenient().when(eventRepository.searchByTitle(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(damageAssessmentRepository.searchByAddress(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(teamRepository.searchByCode(any(), any(), any())).thenReturn(List.of());
        lenient().when(resourceRequestRepository.searchByTitle(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(userRepository.searchScoped(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(districtRepository.searchByName(any(), any())).thenReturn(List.of());
        lenient().when(neighborhoodRepository.searchByName(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("2 karakterden kısa sorgu hiçbir repository'yi çağırmadan boş sonuç döner")
    void tooShortQuery_returnsEmptyWithoutQuerying() {
        GlobalSearchResponse response = service.search("a", principal(UserRole.ADMIN, null, null));

        assertThat(response.results()).isEmpty();
        verify(eventRepository, never()).searchByTitle(any(), any(), any(), any());
        verify(userRepository, never()).searchScoped(any(), any(), any(), any());
    }

    @Test
    @DisplayName("VOLUNTEER hiçbir zaman USER kategorisinde sonuç alamaz")
    void volunteer_neverSearchesUsers() {
        service.search("kadikoy", principal(UserRole.VOLUNTEER, null, null));

        verify(userRepository, never()).searchScoped(any(), any(), any(), any());
    }

    @Test
    @DisplayName("ADMIN dışındaki roller USER kategorisinde arayabilir (district coordinator)")
    void districtCoordinator_canSearchUsers() {
        UUID districtId = UUID.randomUUID();
        when(districtRepository.findByCoordinatorId(any())).thenReturn(Optional.empty());

        service.search("ahmet", principal(UserRole.DISTRICT_COORDINATOR, districtId, null));

        verify(userRepository).searchScoped(eq("ahmet"), eq(districtId), isNull(), any());
    }

    @Test
    @DisplayName("DISTRICT_COORDINATOR araması kendi ilçesiyle sınırlanır")
    void districtCoordinator_scopedToOwnDistrict() {
        UUID districtId = UUID.randomUUID();
        District district = District.builder().id(districtId).name("Kadıköy").build();
        when(districtRepository.findByCoordinatorId(any())).thenReturn(Optional.of(district));

        service.search("kadikoy", principal(UserRole.DISTRICT_COORDINATOR, UUID.randomUUID(), null));

        verify(eventRepository).searchByTitle(eq("kadikoy"), eq(districtId), isNull(), any());
        verify(teamRepository).searchByCode(eq("kadikoy"), eq(districtId), any());
    }

    @Test
    @DisplayName("NEIGHBORHOOD_COORDINATOR araması kendi mahallesiyle sınırlanır")
    void neighborhoodCoordinator_scopedToOwnNeighborhood() {
        UUID neighborhoodId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        Neighborhood neighborhood = Neighborhood.builder().id(neighborhoodId).name("Moda").build();
        when(neighborhoodRepository.findByCoordinatorId(any())).thenReturn(Optional.of(neighborhood));

        service.search("moda", principal(UserRole.NEIGHBORHOOD_COORDINATOR, districtId, UUID.randomUUID()));

        verify(damageAssessmentRepository).searchByAddress(eq("moda"), eq(districtId), eq(neighborhoodId), any());
    }

    @Test
    @DisplayName("ADMIN kapsamsız (tüm ilçeler) arama yapar")
    void admin_unrestrictedSearch() {
        service.search("olay", principal(UserRole.ADMIN, null, null));

        verify(eventRepository).searchByTitle(eq("olay"), isNull(), isNull(), any());
        verify(userRepository).searchScoped(eq("olay"), isNull(), isNull(), any());
    }
}
