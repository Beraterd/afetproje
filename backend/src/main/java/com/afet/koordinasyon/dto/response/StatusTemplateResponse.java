package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusTemplateResponse {
    private String key;
    private String label;
}
