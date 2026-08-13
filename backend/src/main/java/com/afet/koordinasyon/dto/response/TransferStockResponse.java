package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferStockResponse {
    private ResourceStockResponse source;
    private ResourceStockResponse target;
}
