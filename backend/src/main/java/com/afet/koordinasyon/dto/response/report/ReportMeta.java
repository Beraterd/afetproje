package com.afet.koordinasyon.dto.response.report;

import lombok.Builder;

/**
 * Tüm rapor türleri için ortak başlık/meta bilgisi: tür, dönem, ilçe/mahalle kapsamı, hazırlayan.
 */
@Builder
public record ReportMeta(
        String type,             // ReportType.name()
        String typeLabel,        // "Günlük Rapor" vb.
        String periodLabel,      // "12.06.2026" / "06.06.2026 - 12.06.2026" / "Son 30 gün" vb.
        String from,             // ISO başlangıç (dahil)
        String to,               // ISO bitiş (hariç)
        String districtId,
        String districtName,
        String neighborhoodId,
        String neighborhoodName,
        String scopeLabel,       // "Sistem Geneli" / "İlçe" / "Mahalle"
        String generatedBy,
        String role,
        String generatedAt
) {}
