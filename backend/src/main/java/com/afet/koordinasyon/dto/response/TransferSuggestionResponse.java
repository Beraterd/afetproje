package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Deterministik kaynak-transfer önerisi — aynı kategoride güvenli fazlalığı olan bir kaynak stok. */
@Data
@Builder
public class TransferSuggestionResponse {
    private UUID sourceStockId;
    private String sourceLabel;
    private String sourceDistrictName;
    private String sourceNeighborhoodName;
    private int availableQuantity;
    private int suggestedQuantity;
    private String unit;
    private String reason;
}
