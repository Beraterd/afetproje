package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bina detayında gösterilen en güncel hasar kaydı özeti — BİLEREK reportedBy/verifiedBy/
 * approvedBy veya herhangi bir vatandaş kişisel verisi (isim/telefon/e-posta) İÇERMEZ.
 * DamageAssessmentResponse'u (bu alanları taşıyan) burada yeniden kullanma/genişletme —
 * bina detay yüzeyi bu PII sızıntısını devralmamalı. Bkz. plan: graceful-coalescing-stearns.md.
 */
@Data
@Builder
public class BuildingLatestAssessmentSummary {
    private UUID id;
    private String damageLevel;
    private String damageLevelLabel;
    private String verificationStatus;
    private String verificationStatusLabel;
    private OffsetDateTime createdAt;
    private List<String> photoUrls;
}
