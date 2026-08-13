package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 3 kademeli hasar durumu öncelik mantığını (constraint 9) doğrular:
 * KOORDINATOR_ONAYLADI > SAHADA_DOGRULANDI > doğrulanmamış vatandaş bildirimi,
 * her kademe içinde en yüksek DamageLevel seçilir.
 */
@ExtendWith(MockitoExtension.class)
class BuildingDamageStatusServiceTest {

    @Mock private DamageAssessmentRepository damageAssessmentRepository;
    @InjectMocks private BuildingDamageStatusService service;

    private UUID buildingId;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
    }

    private Object[] row(UUID id, VerificationStatus status, DamageLevel level, long count) {
        return new Object[]{ id, status, level, count };
    }

    @Test
    @DisplayName("Hiç kayıt yoksa damageStatus null döner (rapor yok != UNASSESSED)")
    void noAssessments_returnsNullStatus() {
        when(damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(any())).thenReturn(List.of());

        Map<UUID, BuildingDamageStatusService.BuildingDamageSummary> result =
                service.summarizeForBuildings(List.of(buildingId));

        assertThat(result.get(buildingId).damageStatus()).isNull();
        assertThat(result.get(buildingId).totalCount()).isZero();
        assertThat(result.get(buildingId).verifiedCount()).isZero();
    }

    @Test
    @DisplayName("Yalnızca doğrulanmamış vatandaş bildirimi varsa: en yüksek seviye, verifiedCount=0")
    void onlyUnverifiedReports_usesHighestAmongThem() {
        when(damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(any())).thenReturn(List.of(
                row(buildingId, VerificationStatus.INCELEME_GEREKIYOR, DamageLevel.LIGHT, 1),
                row(buildingId, VerificationStatus.ASSIGNED, DamageLevel.MODERATE, 2)
        ));

        var summary = service.summarizeForBuildings(List.of(buildingId)).get(buildingId);

        assertThat(summary.damageStatus()).isEqualTo(DamageLevel.MODERATE);
        assertThat(summary.totalCount()).isEqualTo(3);
        assertThat(summary.verifiedCount()).isZero();
    }

    @Test
    @DisplayName("SAHADA_DOGRULANDI varsa, daha yüksek olsa bile doğrulanmamış bildirimlerin önüne geçer")
    void fieldVerified_takesPriorityOverUnverifiedEvenIfLower() {
        when(damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(any())).thenReturn(List.of(
                row(buildingId, VerificationStatus.INCELEME_GEREKIYOR, DamageLevel.COLLAPSED, 1),
                row(buildingId, VerificationStatus.SAHADA_DOGRULANDI, DamageLevel.LIGHT, 1)
        ));

        var summary = service.summarizeForBuildings(List.of(buildingId)).get(buildingId);

        assertThat(summary.damageStatus()).isEqualTo(DamageLevel.LIGHT);
        assertThat(summary.verifiedCount()).isEqualTo(1);
        assertThat(summary.totalCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("KOORDINATOR_ONAYLADI varsa her zaman kazanır (en üst öncelik)")
    void coordinatorApproved_alwaysWins() {
        when(damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(any())).thenReturn(List.of(
                row(buildingId, VerificationStatus.SAHADA_DOGRULANDI, DamageLevel.HEAVY, 1),
                row(buildingId, VerificationStatus.KOORDINATOR_ONAYLADI, DamageLevel.MODERATE, 1)
        ));

        var summary = service.summarizeForBuildings(List.of(buildingId)).get(buildingId);

        assertThat(summary.damageStatus()).isEqualTo(DamageLevel.MODERATE);
        assertThat(summary.verifiedCount()).isEqualTo(2); // SAHADA_DOGRULANDI + KOORDINATOR_ONAYLADI ikisi de "verified"
    }

    @Test
    @DisplayName("Aynı kademede en yüksek DamageLevel seçilir (COLLAPSED > HEAVY)")
    void withinSameTier_picksHighestLevel() {
        when(damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(any())).thenReturn(List.of(
                row(buildingId, VerificationStatus.KOORDINATOR_ONAYLADI, DamageLevel.HEAVY, 3),
                row(buildingId, VerificationStatus.KOORDINATOR_ONAYLADI, DamageLevel.COLLAPSED, 1)
        ));

        var summary = service.summarizeForBuildings(List.of(buildingId)).get(buildingId);

        assertThat(summary.damageStatus()).isEqualTo(DamageLevel.COLLAPSED);
        assertThat(summary.totalCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Birden fazla bina bağımsız özetlenir (çoklu kullanıcı/çoklu bina aggregation)")
    void multipleBuildings_summarizedIndependently() {
        UUID buildingA = UUID.randomUUID();
        UUID buildingB = UUID.randomUUID();
        when(damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(any())).thenReturn(List.of(
                row(buildingA, VerificationStatus.INCELEME_GEREKIYOR, DamageLevel.LIGHT, 2),
                row(buildingB, VerificationStatus.KOORDINATOR_ONAYLADI, DamageLevel.COLLAPSED, 1)
        ));

        Map<UUID, BuildingDamageStatusService.BuildingDamageSummary> result =
                service.summarizeForBuildings(List.of(buildingA, buildingB));

        assertThat(result.get(buildingA).damageStatus()).isEqualTo(DamageLevel.LIGHT);
        assertThat(result.get(buildingA).totalCount()).isEqualTo(2);
        assertThat(result.get(buildingB).damageStatus()).isEqualTo(DamageLevel.COLLAPSED);
        assertThat(result.get(buildingB).verifiedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Boş id listesi verilirse boş map döner (repository sorgulanmaz gerekmez ama sorgulanırsa da tolere edilir)")
    void emptyBuildingIds_returnsEmptyMap() {
        Map<UUID, BuildingDamageStatusService.BuildingDamageSummary> result =
                service.summarizeForBuildings(List.of());
        assertThat(result).isEmpty();
    }
}
