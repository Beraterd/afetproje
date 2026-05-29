package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private UUID actorId;
    private String actorName;
    private String actorRole;
    private String action;
    private String entityType;
    private UUID entityId;
    private String description;
    private String metadata;
    private String ipAddress;
    private String userAgent;
    private boolean isSystemAction;
    private OffsetDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .isSystemAction(log.isSystemAction())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
