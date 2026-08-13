package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.enums.DamageLevel;
import com.afet.koordinasyon.domain.enums.VerificationStatus;
import com.afet.koordinasyon.repository.DamageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Bir binanın "hasar durumu"nu, o binaya bağlı DamageAssessment kayıtlarından CANLI türetir —
 * Building üzerinde ayrı, mutable bir durum sütunu tutulmaz (tek doğruluk kaynağı DamageAssessment
 * kalır, invalidation hatası riski olmaz). 22k binalık pilot ölçeğinde, bbox sorgusunun döndürdüğü
 * ~birkaç yüz/bin bina için tek gruplu sorgu maliyeti düşüktür (bkz. plan: graceful-coalescing-stearns.md).
 *
 * Öncelik (constraint 9, kullanıcı spesifikasyonu):
 *   1) KOORDINATOR_ONAYLADI kayıtlar arasında en yüksek DamageLevel
 *   2) yoksa SAHADA_DOGRULANDI kayıtlar arasında en yüksek DamageLevel
 *   3) yoksa kalan (INCELEME_GEREKIYOR/ASSIGNED, yani doğrulanmamış vatandaş bildirimi) arasında en yüksek
 *   4) hiç kayıt yoksa: null (rapor yok — bu, UNASSESSED'ten farklı bir durum: "raporlandı ama henüz değerlendirilmedi" değil, "hiç rapor yok")
 */
@Service
@RequiredArgsConstructor
public class BuildingDamageStatusService {

    public record BuildingDamageSummary(DamageLevel damageStatus, int totalCount, int verifiedCount) {}

    private final DamageAssessmentRepository damageAssessmentRepository;

    public Map<UUID, BuildingDamageSummary> summarizeForBuildings(Collection<UUID> buildingIds) {
        Map<UUID, BuildingDamageSummary> result = new HashMap<>();
        if (buildingIds == null || buildingIds.isEmpty()) return result;

        // buildingId -> tier -> (highest DamageLevel ordinal seen, count in tier)
        Map<UUID, EnumMap<VerificationStatus, DamageLevel>> highestByTier = new HashMap<>();
        Map<UUID, Integer> totalCounts = new HashMap<>();
        Map<UUID, Integer> verifiedCounts = new HashMap<>();

        List<Object[]> rows = damageAssessmentRepository.findDamageStatusGroupsByBuildingIds(buildingIds);
        for (Object[] row : rows) {
            UUID buildingId = (UUID) row[0];
            VerificationStatus status = (VerificationStatus) row[1];
            DamageLevel level = (DamageLevel) row[2];
            long count = (Long) row[3];

            totalCounts.merge(buildingId, (int) count, Integer::sum);
            if (status == VerificationStatus.KOORDINATOR_ONAYLADI || status == VerificationStatus.SAHADA_DOGRULANDI) {
                verifiedCounts.merge(buildingId, (int) count, Integer::sum);
            }

            highestByTier.computeIfAbsent(buildingId, k -> new EnumMap<>(VerificationStatus.class))
                    .merge(status, level, (a, b) -> a.ordinal() >= b.ordinal() ? a : b);
        }

        for (UUID buildingId : buildingIds) {
            EnumMap<VerificationStatus, DamageLevel> tiers = highestByTier.get(buildingId);
            int total = totalCounts.getOrDefault(buildingId, 0);
            int verified = verifiedCounts.getOrDefault(buildingId, 0);

            DamageLevel status = null;
            if (tiers != null) {
                if (tiers.containsKey(VerificationStatus.KOORDINATOR_ONAYLADI)) {
                    status = tiers.get(VerificationStatus.KOORDINATOR_ONAYLADI);
                } else if (tiers.containsKey(VerificationStatus.SAHADA_DOGRULANDI)) {
                    status = tiers.get(VerificationStatus.SAHADA_DOGRULANDI);
                } else {
                    status = tiers.values().stream()
                            .max(Comparator.comparingInt(DamageLevel::ordinal))
                            .orElse(null);
                }
            }
            result.put(buildingId, new BuildingDamageSummary(status, total, verified));
        }
        return result;
    }
}
