package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.Building;
import com.afet.koordinasyon.domain.entity.DamageAssessment;
import com.afet.koordinasyon.domain.entity.DamageAssessmentAssignment;
import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.BuildingSource;
import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import com.afet.koordinasyon.dto.request.AssignDamageAssessmentRequest;
import com.afet.koordinasyon.dto.request.CreateDamageAssessmentRequest;
import com.afet.koordinasyon.dto.request.VerifyDamageAssessmentRequest;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.ai.DamageAiQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DamageAssessment doğrulama/görevlendirme akışının kritik state transition ve RBAC/scope
 * davranışlarını kapsar — önceden bu servis için hiç test yoktu.
 */
@ExtendWith(MockitoExtension.class)
class DamageAssessmentServiceTest {

    @Mock private DamageAssessmentRepository damageAssessmentRepository;
    @Mock private DamageAssessmentPhotoRepository damageAssessmentPhotoRepository;
    @Mock private DamageAssessmentAssignmentRepository assignmentRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventVolunteerRepository eventVolunteerRepository;
    @Mock private DamageAssessmentAiService damageAssessmentAiService;
    @Mock private DamageAiQueueService damageAiQueueService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditLogService auditLogService;
    @Mock private BuildingRepository buildingRepository;

    @InjectMocks private DamageAssessmentService service;

    private District district;
    private Neighborhood neighborhood;
    private DamageAssessment assessment;
    private User actor;

    @BeforeEach
    void setUp() {
        district = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        neighborhood = Neighborhood.builder().id(UUID.randomUUID()).name("Moda").district(district).build();
        assessment = DamageAssessment.builder()
                .id(UUID.randomUUID())
                .district(district)
                .neighborhood(neighborhood)
                .address("Test Sokak No:1")
                .damageLevel(DamageLevel.HEAVY)
                .verificationStatus(VerificationStatus.SAHADA_DOGRULANDI)
                .build();
        actor = new User();
        actor.setId(UUID.randomUUID());
        actor.setFirstName("Actor");
        actor.setLastName("User");
    }

    private UserPrincipal principal(UserRole role, UUID districtId, UUID neighborhoodId) {
        return new UserPrincipal(UUID.randomUUID(), "Test", "User", "t@x.com",
                null, role, districtId, neighborhoodId, true, false, List.of());
    }

    private VerifyDamageAssessmentRequest verifyReq(VerificationStatus status) {
        VerifyDamageAssessmentRequest req = new VerifyDamageAssessmentRequest();
        req.setVerificationStatus(status);
        return req;
    }

    // ── verify() — yetkisiz kullanıcı ───────────────────────────────────────────

    @Test
    @DisplayName("Gönüllü doğrulama yapamaz")
    void verify_volunteer_throws() {
        UserPrincipal volunteer = principal(UserRole.VOLUNTEER, null, null);

        assertThatThrownBy(() -> service.verify(assessment.getId(), verifyReq(VerificationStatus.SAHADA_DOGRULANDI), volunteer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Gönüllüler doğrulama yapamaz");

        verify(damageAssessmentRepository, never()).save(any());
    }

    // ── verify() — scope dışı kayıt ─────────────────────────────────────────────

    @Test
    @DisplayName("İlçe koordinatörü başka ilçenin kaydını doğrulayamaz")
    void verify_districtCoordinator_wrongDistrict_throws() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        UserPrincipal dc = principal(UserRole.DISTRICT_COORDINATOR, UUID.randomUUID(), null); // farklı ilçe

        assertThatThrownBy(() -> service.verify(assessment.getId(), verifyReq(VerificationStatus.KOORDINATOR_ONAYLADI), dc))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sadece kendi ilçenizdeki");

        verify(damageAssessmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mahalle koordinatörü başka mahallenin kaydını doğrulayamaz")
    void verify_neighborhoodCoordinator_wrongNeighborhood_throws() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        UserPrincipal nc = principal(UserRole.NEIGHBORHOOD_COORDINATOR, null, UUID.randomUUID()); // farklı mahalle

        assertThatThrownBy(() -> service.verify(assessment.getId(), verifyReq(VerificationStatus.SAHADA_DOGRULANDI), nc))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sadece kendi mahallenizin");
    }

    // ── verify() — geçerli state transition ─────────────────────────────────────

    @Test
    @DisplayName("Sahada doğrulama: verifiedBy/verifiedAt set edilir")
    void verify_fieldVerification_setsVerifiedByAndAt() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        when(damageAssessmentRepository.save(any(DamageAssessment.class))).thenAnswer(i -> i.getArgument(0));
        UserPrincipal nc = principal(UserRole.NEIGHBORHOOD_COORDINATOR, null, neighborhood.getId());

        service.verify(assessment.getId(), verifyReq(VerificationStatus.SAHADA_DOGRULANDI), nc);

        assertThat(assessment.getVerifiedBy()).isEqualTo(actor);
        assertThat(assessment.getVerifiedAt()).isNotNull();
        assertThat(assessment.getVerificationStatus()).isEqualTo(VerificationStatus.SAHADA_DOGRULANDI);
    }

    @Test
    @DisplayName("Doğrulama başarılı olduğunda audit log kaydedilir (aktör + yeni durum)")
    void verify_success_writesAuditLog() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        when(damageAssessmentRepository.save(any(DamageAssessment.class))).thenAnswer(i -> i.getArgument(0));
        UserPrincipal nc = principal(UserRole.NEIGHBORHOOD_COORDINATOR, null, neighborhood.getId());

        service.verify(assessment.getId(), verifyReq(VerificationStatus.SAHADA_DOGRULANDI), nc);

        verify(auditLogService).logUserAction(
                eq(nc),
                eq(com.afet.koordinasyon.domain.enums.AuditActionType.DAMAGE_ASSESSMENT_VERIFIED),
                eq("DamageAssessment"),
                eq(assessment.getId()),
                any(String.class),
                any());
    }

    @Test
    @DisplayName("Mahalle koordinatörü koordinatör onayı veremez (yalnızca ilçe koord./admin)")
    void verify_neighborhoodCoordinator_cannotGiveFinalApproval() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        UserPrincipal nc = principal(UserRole.NEIGHBORHOOD_COORDINATOR, null, neighborhood.getId());

        assertThatThrownBy(() -> service.verify(assessment.getId(), verifyReq(VerificationStatus.KOORDINATOR_ONAYLADI), nc))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("yalnızca ilçe koordinatörü veya admin");

        verify(damageAssessmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("İlçe koordinatörü kendi ilçesinde koordinatör onayı verebilir")
    void verify_districtCoordinator_ownDistrict_finalApprovalSucceeds() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        when(damageAssessmentRepository.save(any(DamageAssessment.class))).thenAnswer(i -> i.getArgument(0));
        UserPrincipal dc = principal(UserRole.DISTRICT_COORDINATOR, district.getId(), null);

        service.verify(assessment.getId(), verifyReq(VerificationStatus.KOORDINATOR_ONAYLADI), dc);

        assertThat(assessment.getApprovedBy()).isEqualTo(actor);
        assertThat(assessment.getApprovedAt()).isNotNull();
        assertThat(assessment.getVerificationStatus()).isEqualTo(VerificationStatus.KOORDINATOR_ONAYLADI);
    }

    // ── assign() — duplicate assignment ─────────────────────────────────────────

    @Test
    @DisplayName("Aynı kullanıcı aynı hasar tespitine iki kez atanamaz")
    void assign_duplicateActiveAssignment_throws() {
        assessment.setVerificationStatus(VerificationStatus.INCELEME_GEREKIYOR);
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        UserPrincipal admin = principal(UserRole.ADMIN, null, null);
        UUID assigneeId = UUID.randomUUID();
        when(assignmentRepository.existsByDamageAssessmentIdAndUserIdAndActiveTrue(assessment.getId(), assigneeId))
                .thenReturn(true);

        AssignDamageAssessmentRequest req = new AssignDamageAssessmentRequest();
        req.setUserId(assigneeId);

        assertThatThrownBy(() -> service.assign(assessment.getId(), req, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("zaten bu hasar tespitine atanmış");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Atama başarılı olduğunda INCELEME_GEREKIYOR -> ASSIGNED durumuna otomatik geçer")
    void assign_success_autoTransitionsToAssigned() {
        assessment.setVerificationStatus(VerificationStatus.INCELEME_GEREKIYOR);
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        UserPrincipal admin = principal(UserRole.ADMIN, null, null);
        UUID assigneeId = UUID.randomUUID();
        User assignee = new User();
        assignee.setId(assigneeId);
        when(assignmentRepository.existsByDamageAssessmentIdAndUserIdAndActiveTrue(assessment.getId(), assigneeId))
                .thenReturn(false);
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(actor));
        when(assignmentRepository.save(any(DamageAssessmentAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(damageAssessmentRepository.save(any(DamageAssessment.class))).thenAnswer(i -> i.getArgument(0));

        AssignDamageAssessmentRequest req = new AssignDamageAssessmentRequest();
        req.setUserId(assigneeId);
        service.assign(assessment.getId(), req, admin);

        assertThat(assessment.getVerificationStatus()).isEqualTo(VerificationStatus.ASSIGNED);
    }

    @Test
    @DisplayName("İlçe koordinatörü başka ilçenin kaydına görevli atayamaz")
    void assign_districtCoordinator_wrongDistrict_throws() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        UserPrincipal dc = principal(UserRole.DISTRICT_COORDINATOR, UUID.randomUUID(), null);

        AssignDamageAssessmentRequest req = new AssignDamageAssessmentRequest();
        req.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> service.assign(assessment.getId(), req, dc))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sadece kendi ilçenizdeki");

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gönüllü görevli atayamaz")
    void assign_volunteer_throws() {
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        UserPrincipal volunteer = principal(UserRole.VOLUNTEER, null, null);
        AssignDamageAssessmentRequest req = new AssignDamageAssessmentRequest();
        req.setUserId(UUID.randomUUID());

        assertThatThrownBy(() -> service.assign(assessment.getId(), req, volunteer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Gönüllüler görevli atayamaz");
    }

    // ── removeAssignment() — state geri dönüşü ──────────────────────────────────

    @Test
    @DisplayName("Son aktif atama kaldırılınca ASSIGNED -> INCELEME_GEREKIYOR durumuna geri döner")
    void removeAssignment_lastOne_revertsStatus() {
        assessment.setVerificationStatus(VerificationStatus.ASSIGNED);
        UUID assignmentId = UUID.randomUUID();
        DamageAssessmentAssignment assignment = DamageAssessmentAssignment.builder()
                .id(assignmentId).damageAssessment(assessment).active(true).build();

        UserPrincipal admin = principal(UserRole.ADMIN, null, null);
        when(assignmentRepository.findByIdAndDamageAssessmentIdAndActiveTrue(assignmentId, assessment.getId()))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(any(DamageAssessmentAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(damageAssessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(assignmentRepository.findByDamageAssessmentIdAndActiveTrue(assessment.getId())).thenReturn(List.of());
        when(damageAssessmentRepository.save(any(DamageAssessment.class))).thenAnswer(i -> i.getArgument(0));

        service.removeAssignment(assessment.getId(), assignmentId, admin);

        assertThat(assignment.isActive()).isFalse();
        assertThat(assessment.getVerificationStatus()).isEqualTo(VerificationStatus.INCELEME_GEREKIYOR);
    }

    // ── create() — Building linkage (Pendik 3B pilotu) ──────────────────────────

    @Nested
    @DisplayName("create() — buildingId ile bina bağlama")
    class BuildingLinking {

        private CreateDamageAssessmentRequest baseRequest() {
            CreateDamageAssessmentRequest req = new CreateDamageAssessmentRequest();
            req.setNeighborhoodId(neighborhood.getId());
            req.setAddress("İstemciden gelen adres (yok sayılmalı)");
            req.setLatitude(1.111111);
            req.setLongitude(2.222222);
            req.setStreetName("İstemci Sokak");
            req.setBuildingNo("İstemci No");
            req.setDamageLevel(DamageLevel.MODERATE);
            return req;
        }

        private MockMultipartFile validPhoto() {
            return new MockMultipartFile("photo", "test.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());
        }

        private Building building(Neighborhood nb) {
            return Building.builder()
                    .id(UUID.randomUUID())
                    .source(BuildingSource.OPENSTREETMAP)
                    .externalId("way/1")
                    .district(nb.getDistrict())
                    .neighborhood(nb)
                    .streetName("Gerçek Bina Sokak")
                    .buildingNumber("12")
                    .displayAddress("Gerçek Bina Sokak 12, Moda Mah., Kadıköy/İstanbul")
                    .latitude(BigDecimal.valueOf(40.981111))
                    .longitude(BigDecimal.valueOf(29.021111))
                    .build();
        }

        @Test
        @DisplayName("Var olmayan buildingId reddedilir (ResourceNotFoundException), kayıt oluşturulmaz")
        void create_invalidBuildingId_throws() {
            UUID missingBuildingId = UUID.randomUUID();
            CreateDamageAssessmentRequest req = baseRequest();
            req.setBuildingId(missingBuildingId);

            when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
            when(buildingRepository.findById(missingBuildingId)).thenReturn(Optional.empty());

            UserPrincipal volunteer = principal(UserRole.VOLUNTEER, null, null);
            assertThatThrownBy(() -> service.create(req, List.of(validPhoto()), volunteer))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(damageAssessmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Seçilen bina, belirtilen mahalleyle eşleşmiyorsa reddedilir")
        void create_buildingNeighborhoodMismatch_throws() {
            Neighborhood otherNeighborhood = Neighborhood.builder()
                    .id(UUID.randomUUID()).name("Başka Mahalle").district(district).build();
            Building mismatchedBuilding = building(otherNeighborhood);

            CreateDamageAssessmentRequest req = baseRequest(); // neighborhoodId = neighborhood.getId()
            req.setBuildingId(mismatchedBuilding.getId());

            when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
            when(buildingRepository.findById(mismatchedBuilding.getId())).thenReturn(Optional.of(mismatchedBuilding));

            UserPrincipal volunteer = principal(UserRole.VOLUNTEER, null, null);
            assertThatThrownBy(() -> service.create(req, List.of(validPhoto()), volunteer))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("mahalle ile eşleşmiyor");

            verify(damageAssessmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Geçerli buildingId: adres/koordinat/sokak istemciden değil Building kaydından türetilir")
        void create_validBuildingId_overridesAddressFromBuilding() {
            Building b = building(neighborhood);
            CreateDamageAssessmentRequest req = baseRequest();
            req.setBuildingId(b.getId());

            when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
            when(buildingRepository.findById(b.getId())).thenReturn(Optional.of(b));
            when(userRepository.findById(any())).thenReturn(Optional.of(actor));
            // save() sonrası akış (fotoğraf yazma, AI kuyruğu, transaction-sync) bu testin kapsamı
            // dışında — argümanı yakalayıp akışı burada kasıtlı olarak kesiyoruz.
            when(damageAssessmentRepository.save(any(DamageAssessment.class)))
                    .thenThrow(new RuntimeException("stop-after-capture"));

            UserPrincipal volunteer = principal(UserRole.VOLUNTEER, null, null);
            assertThatThrownBy(() -> service.create(req, List.of(validPhoto()), volunteer))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("stop-after-capture");

            ArgumentCaptor<DamageAssessment> captor = ArgumentCaptor.forClass(DamageAssessment.class);
            verify(damageAssessmentRepository).save(captor.capture());
            DamageAssessment saved = captor.getValue();

            assertThat(saved.getBuilding()).isEqualTo(b);
            assertThat(saved.getAddress()).isEqualTo(b.getDisplayAddress());
            assertThat(saved.getStreetName()).isEqualTo(b.getStreetName());
            assertThat(saved.getBuildingNo()).isEqualTo(b.getBuildingNumber());
            assertThat(saved.getLatitude()).isEqualByComparingTo(b.getLatitude());
            assertThat(saved.getLongitude()).isEqualByComparingTo(b.getLongitude());
            assertThat(saved.isLocationVerified()).isTrue();
        }

        @Test
        @DisplayName("buildingId olmayan eski akış hâlâ çalışır: istemci address/lat/lng aynen kullanılır")
        void create_withoutBuildingId_usesRequestAddressUnchanged() {
            CreateDamageAssessmentRequest req = baseRequest(); // buildingId set edilmedi (null)

            when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
            when(userRepository.findById(any())).thenReturn(Optional.of(actor));
            when(damageAssessmentRepository.save(any(DamageAssessment.class)))
                    .thenThrow(new RuntimeException("stop-after-capture"));

            UserPrincipal volunteer = principal(UserRole.VOLUNTEER, null, null);
            assertThatThrownBy(() -> service.create(req, List.of(validPhoto()), volunteer))
                    .isInstanceOf(RuntimeException.class);

            verify(buildingRepository, never()).findById(any());

            ArgumentCaptor<DamageAssessment> captor = ArgumentCaptor.forClass(DamageAssessment.class);
            verify(damageAssessmentRepository).save(captor.capture());
            DamageAssessment saved = captor.getValue();

            assertThat(saved.getBuilding()).isNull();
            assertThat(saved.getAddress()).isEqualTo(req.getAddress());
            assertThat(saved.getStreetName()).isEqualTo(req.getStreetName());
            assertThat(saved.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(req.getLatitude()));
        }
    }
}
