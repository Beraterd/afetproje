package com.afet.koordinasyon.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a real bug found via live testing: Lombok generates `isEstimatedHeight()`
 * for a boolean field already named `isEstimatedHeight`, and Jackson's default property-naming
 * strips the "is" prefix from boolean getters — producing a wire property named "estimatedHeight"
 * instead of "isEstimatedHeight", silently breaking `BuildingDetailsPanel.tsx`'s
 * `query.data.isEstimatedHeight` read (always undefined/falsy). Fixed with an explicit
 * `@JsonProperty("isEstimatedHeight")`; this test locks the wire contract in place.
 */
class BuildingResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildingSummaryResponse_serializesIsEstimatedHeightWithTheExpectedKey() throws Exception {
        BuildingSummaryResponse response = BuildingSummaryResponse.builder()
                .estimatedHeightM(6.0)
                .isEstimatedHeight(true)
                .heightSource("fallback")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"isEstimatedHeight\":true");
        assertThat(json).doesNotContain("\"estimatedHeight\":");
    }

    @Test
    void buildingDetailResponse_serializesIsEstimatedHeightWithTheExpectedKey() throws Exception {
        BuildingDetailResponse response = BuildingDetailResponse.builder()
                .estimatedHeightM(6.0)
                .isEstimatedHeight(true)
                .heightSource("fallback")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"isEstimatedHeight\":true");
        assertThat(json).doesNotContain("\"estimatedHeight\":");
    }
}
