package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoordinatorServiceTest {

    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CoordinatorService coordinatorService;

    private UUID districtId;
    private UUID neighborhoodId;
    private UUID userId;
    private District district;
    private Neighborhood neighborhood;
    private User user;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        districtId     = UUID.randomUUID();
        neighborhoodId = UUID.randomUUID();
        userId         = UUID.randomUUID();

        district = District.builder().id(districtId).name("Kadıköy").build();

        neighborhood = Neighborhood.builder()
                .id(neighborhoodId)
                .name("Moda")
                .district(district)
                .build();

        user = User.builder()
                .id(userId)
                .firstName("Ali")
                .lastName("Veli")
                .email("ali@test.com")
                .role(UserRole.VOLUNTEER)
                .build();

        adminPrincipal = mock(UserPrincipal.class);
        when(adminPrincipal.getRole()).thenReturn(UserRole.ADMIN);
    }

    // ── assignDistrictCoordinator ──────────────────────────────────────────

    @Test
    void assignDistrictCoordinator_nonAdmin_throwsBusinessRuleException() {
        UserPrincipal dcPrincipal = mock(UserPrincipal.class);
        when(dcPrincipal.getRole()).thenReturn(UserRole.DISTRICT_COORDINATOR);

        assertThatThrownBy(() ->
                coordinatorService.assignDistrictCoordinator(districtId, userId, dcPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("yönetici");
    }

    @Test
    void assignDistrictCoordinator_promotesUserAndSetsDistrict() {
        when(districtRepository.findById(districtId)).thenReturn(Optional.of(district));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(districtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignDistrictCoordinator(districtId, userId, adminPrincipal);

        assertThat(user.getRole()).isEqualTo(UserRole.DISTRICT_COORDINATOR);
        assertThat(user.getDistrict()).isEqualTo(district);
        assertThat(district.getCoordinator()).isEqualTo(user);
    }

    @Test
    void assignDistrictCoordinator_clearsUserPreviousDistrictReference() {
        UUID oldDistrictId = UUID.randomUUID();
        District oldDistrict = District.builder().id(oldDistrictId).name("Beşiktaş").coordinator(user).build();

        when(districtRepository.findById(districtId)).thenReturn(Optional.of(district));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.of(oldDistrict));
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(districtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignDistrictCoordinator(districtId, userId, adminPrincipal);

        assertThat(oldDistrict.getCoordinator()).isNull();
    }

    @Test
    void assignDistrictCoordinator_clearsUserPreviousNeighborhoodReference() {
        Neighborhood oldNeighborhood = Neighborhood.builder()
                .id(UUID.randomUUID()).name("Eski Mahalle").district(district).coordinator(user).build();

        when(districtRepository.findById(districtId)).thenReturn(Optional.of(district));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.of(oldNeighborhood));
        when(districtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignDistrictCoordinator(districtId, userId, adminPrincipal);

        assertThat(oldNeighborhood.getCoordinator()).isNull();
    }

    @Test
    void assignDistrictCoordinator_demotesOldCoordinator() {
        UUID oldCoordId = UUID.randomUUID();
        User oldCoord = User.builder()
                .id(oldCoordId)
                .role(UserRole.DISTRICT_COORDINATOR)
                .build();
        district.setCoordinator(oldCoord);

        when(districtRepository.findById(districtId)).thenReturn(Optional.of(district));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(districtRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignDistrictCoordinator(districtId, userId, adminPrincipal);

        assertThat(oldCoord.getRole()).isEqualTo(UserRole.VOLUNTEER);
    }

    // ── assignNeighborhoodCoordinator ─────────────────────────────────────

    @Test
    void assignNeighborhoodCoordinator_districtCoordForDifferentDistrict_throwsException() {
        UUID otherDistrictId = UUID.randomUUID();
        District otherDistrict = District.builder().id(otherDistrictId).name("Beşiktaş").build();
        Neighborhood otherNeighborhood = Neighborhood.builder()
                .id(neighborhoodId).name("Levent").district(otherDistrict).build();

        UserPrincipal dcPrincipal = mock(UserPrincipal.class);
        when(dcPrincipal.getRole()).thenReturn(UserRole.DISTRICT_COORDINATOR);
        when(dcPrincipal.getDistrictId()).thenReturn(districtId); // different from otherDistrictId

        when(neighborhoodRepository.findById(neighborhoodId)).thenReturn(Optional.of(otherNeighborhood));

        assertThatThrownBy(() ->
                coordinatorService.assignNeighborhoodCoordinator(neighborhoodId, userId, dcPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("kendi ilçe");
    }

    @Test
    void assignNeighborhoodCoordinator_districtCoordForOwnDistrict_succeeds() {
        UserPrincipal dcPrincipal = mock(UserPrincipal.class);
        when(dcPrincipal.getRole()).thenReturn(UserRole.DISTRICT_COORDINATOR);
        when(dcPrincipal.getDistrictId()).thenReturn(districtId);

        when(neighborhoodRepository.findById(neighborhoodId)).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignNeighborhoodCoordinator(neighborhoodId, userId, dcPrincipal);

        assertThat(user.getRole()).isEqualTo(UserRole.NEIGHBORHOOD_COORDINATOR);
        assertThat(neighborhood.getCoordinator()).isEqualTo(user);
    }

    @Test
    void assignNeighborhoodCoordinator_clearsUserPreviousDistrictCoordRef() {
        District prevDistrict = District.builder().id(UUID.randomUUID()).name("Eski İlçe").coordinator(user).build();

        when(neighborhoodRepository.findById(neighborhoodId)).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.of(prevDistrict));
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(districtRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignNeighborhoodCoordinator(neighborhoodId, userId, adminPrincipal);

        assertThat(prevDistrict.getCoordinator()).isNull();
        assertThat(user.getRole()).isEqualTo(UserRole.NEIGHBORHOOD_COORDINATOR);
    }

    @Test
    void assignNeighborhoodCoordinator_clearsUserPreviousNeighborhoodRef() {
        UUID oldNbId = UUID.randomUUID();
        Neighborhood oldNb = Neighborhood.builder()
                .id(oldNbId).name("Eski Mahalle").district(district).coordinator(user).build();

        when(neighborhoodRepository.findById(neighborhoodId)).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.of(oldNb));
        when(neighborhoodRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignNeighborhoodCoordinator(neighborhoodId, userId, adminPrincipal);

        assertThat(oldNb.getCoordinator()).isNull();
    }

    @Test
    void assignNeighborhoodCoordinator_userCannotRemainBothDcAndNc() {
        // User was a DC; after becoming NC, their district coordinator ref is cleared
        user.setRole(UserRole.DISTRICT_COORDINATOR);
        District prevDistrict = District.builder()
                .id(UUID.randomUUID()).name("Eski İlçe").coordinator(user).build();

        when(neighborhoodRepository.findById(neighborhoodId)).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(districtRepository.findByCoordinatorId(userId)).thenReturn(Optional.of(prevDistrict));
        when(neighborhoodRepository.findByCoordinatorId(userId)).thenReturn(Optional.empty());
        when(neighborhoodRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(districtRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coordinatorService.assignNeighborhoodCoordinator(neighborhoodId, userId, adminPrincipal);

        // Old district coordinator ref cleared
        assertThat(prevDistrict.getCoordinator()).isNull();
        // User now only has NC role
        assertThat(user.getRole()).isEqualTo(UserRole.NEIGHBORHOOD_COORDINATOR);
    }

    // ── listEligibleUsers ─────────────────────────────────────────────────

    @Test
    void listEligibleUsers_admin_noFilter_returnsAllNonAdminUsers() {
        User volunteer = User.builder().id(UUID.randomUUID()).role(UserRole.VOLUNTEER).email("v@t.com")
                .firstName("V").lastName("V").build();
        User admin = User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).email("a@t.com")
                .firstName("A").lastName("A").build();

        when(userRepository.findAll()).thenReturn(List.of(volunteer, admin));

        var result = coordinatorService.listEligibleUsers(adminPrincipal, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("v@t.com");
    }

    @Test
    void listEligibleUsers_districtCoord_onlyOwnDistrict() {
        UUID dcDistrictId = UUID.randomUUID();
        UserPrincipal dcPrincipal = mock(UserPrincipal.class);
        when(dcPrincipal.getRole()).thenReturn(UserRole.DISTRICT_COORDINATOR);
        when(dcPrincipal.getDistrictId()).thenReturn(dcDistrictId);

        User dcUser = User.builder().id(UUID.randomUUID()).role(UserRole.VOLUNTEER)
                .email("dc@t.com").firstName("D").lastName("C").build();
        when(userRepository.findByDistrictId(dcDistrictId)).thenReturn(List.of(dcUser));

        var result = coordinatorService.listEligibleUsers(dcPrincipal, null, null);

        assertThat(result).hasSize(1);
        verify(userRepository).findByDistrictId(dcDistrictId);
    }
}
