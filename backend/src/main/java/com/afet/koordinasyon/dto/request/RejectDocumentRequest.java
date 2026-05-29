package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectDocumentRequest {
    @NotBlank
    private String reason;
}
