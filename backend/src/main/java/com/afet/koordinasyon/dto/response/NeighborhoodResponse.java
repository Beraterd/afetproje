package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NeighborhoodResponse {
    private UUID id;
    private String name;
    private UUID districtId;
    private String districtName;
    private BigDecimal riskScore;
    private String riskColor;
    private List<AssemblyAreaResponse> assemblyAreas;
}
