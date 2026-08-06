package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.response.DailyReportResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canlı Postgres'e karşı tüm rapor sorgularının çalıştığını doğrular (salt-okunur).
 * Çok sayıda yeni JPQL sayım sorgusu içerdiği için en kritik doğrulamadır.
 */
@SpringBootTest
class ReportServiceIT {

    @Autowired
    private ReportService reportService;

    private UserPrincipal principal(UserRole role, UUID districtId, UUID neighborhoodId) {
        return new UserPrincipal(UUID.randomUUID(), "Test", "Yönetici", "t@x.com",
                null, role, districtId, neighborhoodId, true, false, List.of());
    }

    @Test
    void adminSystemWideReport_runsAndPopulatesMeta() {
        DailyReportResponse r = reportService.generateDaily(null, null, principal(UserRole.ADMIN, null, null));

        assertThat(r).isNotNull();
        assertThat(r.meta().title()).isEqualTo("Afet Yönetim Günlük Faaliyet Raporu");
        assertThat(r.meta().scopeLabel()).isEqualTo("Sistem Geneli Raporu");
        assertThat(r.meta().role()).isEqualTo("Yönetici");
        // Tüm bölümler hesaplanmış olmalı (null değil)
        assertThat(r.resources()).isNotNull();
        assertThat(r.stock()).isNotNull();
        assertThat(r.tasks()).isNotNull();
        assertThat(r.requests()).isNotNull();
        assertThat(r.damage()).isNotNull();
        assertThat(r.assemblyAreas()).isNotNull();
        assertThat(r.volunteers()).isNotNull();
        assertThat(r.communication()).isNotNull();
        assertThat(r.kpis()).isNotEmpty();
    }

    @Test
    void districtCoordinatorReport_isScopedToOwnDistrict() {
        UUID ownDistrict = UUID.randomUUID();
        // İlçe koordinatörü herhangi bir districtId parametresi verse de kendi ilçesine sabitlenir
        DailyReportResponse r = reportService.generateDaily(
                UUID.randomUUID(), null, principal(UserRole.DISTRICT_COORDINATOR, ownDistrict, null));

        assertThat(r).isNotNull();
        assertThat(r.meta().scopeLabel()).isEqualTo("İlçe Raporu");
        assertThat(r.meta().role()).isEqualTo("İlçe Koordinatörü");
    }
}
