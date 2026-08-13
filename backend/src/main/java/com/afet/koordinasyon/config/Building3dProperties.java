package com.afet.koordinasyon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 3B bina altyapısı yapılandırması — hangi ilçelerin bina/3B katmanı aktif olduğunun TEK
 * doğruluk kaynağı. Hiçbir yerde `district.equals("Pendik")` gibi ilçe adı hardcode edilmez;
 * yeni bir ilçenin importu tamamlanıp doğrulandığında yalnızca `enabledDistricts`'e eklenir
 * (bkz. BuildingImportService, AdminBuildingController).
 */
@Component
@ConfigurationProperties(prefix = "app.building3d")
@Getter
@Setter
public class Building3dProperties {

    /** District.name ile eşleştirilir (case-sensitive). */
    private List<String> enabledDistricts = List.of("Pendik");

    /** height_m yok ama building:levels varsa: levels * levelHeightM (metre/kat). */
    private double levelHeightM = 3.0;

    /** Ne height_m ne levels varsa kullanılan sabit tahmini yükseklik (metre). */
    private double fallbackHeightM = 6.0;

    /** Bu zoom seviyesinden itibaren bina footprint'leri gösterilir. */
    private int buildingMinZoom = 15;

    /** Bu zoom seviyesinden itibaren 3B extrusion gösterilir (altında düz 2B footprint). */
    private int extrusionMinZoom = 16;

    public boolean isDistrictEnabled(String districtName) {
        return districtName != null && enabledDistricts.contains(districtName);
    }
}
