package com.afet.koordinasyon.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLocationPermissionRequest {
    /** GRANTED, DENIED veya SKIPPED */
    private String permissionStatus;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;
    /** GPS, Network veya Cell — frontend accuracy değerinden türetilir */
    private String locationSource;
}
