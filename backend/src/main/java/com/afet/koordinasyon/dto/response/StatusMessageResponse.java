package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StatusMessageResponse {
    private UUID id;
    private String templateKey;
    private String messageText;
    private int recipientCount;
    private int sentCount;
    private OffsetDateTime createdAt;
}
