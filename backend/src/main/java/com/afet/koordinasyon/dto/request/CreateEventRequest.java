package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateEventRequest {

    // title is optional — auto-generated as "<TeamName> - <NeighborhoodName>" when blank
    @Size(max = 300)
    private String title;

    private String description;

    @NotNull
    private UUID neighborhoodId;

    /**
     * Team name as enum String (e.g. "HASAR_TESPIT_EKIBI").
     * Preferred over teamId — looked up via TeamRepository.findByName().
     * One of teamName or teamId must be non-null.
     */
    @Size(max = 50)
    private String teamName;

    /**
     * Team UUID — legacy/backward-compatible field.
     * Used when teamName is null.
     */
    private UUID teamId;

    @Min(1)
    private int requiredPeople;
}
