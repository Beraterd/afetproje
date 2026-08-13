package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Bir ilçenin her mahallesi için bina kapsama raporu (bkz. item 7) — bina bulunmayan
 *  mahalleler de buildingCount=0 olarak listeye dahildir, gizlenmez. */
@Data
@Builder
public class NeighborhoodCoverageResponse {
    private UUID neighborhoodId;
    private String neighborhoodName;
    private long buildingCount;
}
