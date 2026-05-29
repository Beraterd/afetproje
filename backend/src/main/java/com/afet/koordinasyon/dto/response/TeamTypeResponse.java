package com.afet.koordinasyon.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight team-type descriptor returned by GET /teams/types.
 * Derived purely from the {@link com.afet.koordinasyon.domain.enums.TeamName} Java enum —
 * no DB query, no Hibernate enum mapping involved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamTypeResponse {
    /** Enum name exactly as stored in DB, e.g. "SEARCH_RESCUE", "HASAR_TESPIT_EKIBI" */
    private String value;
    /** Human-readable Turkish label, e.g. "Arama Kurtarma Ekibi" */
    private String label;
}
