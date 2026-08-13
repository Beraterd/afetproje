package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Olay zaman çizelgesi öğesi — mevcut AuditLog kayıtlarından türetilir, sahte/varsayımsal veri içermez. */
@Data
@Builder
public class EventTimelineItemResponse {
    private UUID id;
    private String type;
    private String title;
    private String description;
    private String actorName;
    private OffsetDateTime createdAt;
}
