package com.afet.koordinasyon.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmergencyStatusSendRequest {
    private String token;
    private String type;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal locationAccuracy;
    /** CURRENT veya LAST_KNOWN */
    private String locationSource;
    private String userAgent;
}
