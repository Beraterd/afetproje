package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodSummaryResponse {
    private UUID id;
    private String name;
    private UUID districtId;
    private String districtName;
}
