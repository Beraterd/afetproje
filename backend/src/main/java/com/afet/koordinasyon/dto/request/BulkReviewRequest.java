package com.afet.koordinasyon.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkReviewRequest {
    private List<UUID> ids;
    private Boolean needsReview;
    private Boolean active;
}
