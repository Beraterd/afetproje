package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** GET /api/streets/search sonuç satırı — item 8. Geometriyi TAŞIMAZ (autocomplete için
 *  gereksiz payload); kamera hedefi için bounds/center yeterlidir. Seçim sonrası highlight
 *  gerekiyorsa GET /api/streets/{id} (bkz. StreetDetailResponse) geometry döner. */
@Data
@Builder
public class StreetSearchResultResponse {
    private UUID id;
    private String name;
    private UUID districtId;
    private String districtName;
    private UUID neighborhoodId;
    private String neighborhoodName;
    private String roadType;
    private StreetBounds bounds;
    private StreetCenter center;
}
