package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Frontend'in `district.equals("Pendik")` gibi hardcode yapmaması için tek config kaynağı. */
@Data
@Builder
public class Building3dConfigResponse {
    private List<String> enabledDistricts;
    private int buildingMinZoom;
    private int extrusionMinZoom;
    /** Tüm binalar tablosundaki toplam kayıt sayısı — frontend bunu "veri seti hiç import
     *  edilmemiş" (0) ile "bu viewport'ta bina yok" (>0 ama bbox sonucu boş) durumlarını
     *  ayırt etmek için kullanır (bkz. Operasyon Haritası / Map3DPage boş durum mesajları). */
    private long totalBuildingCount;
}
