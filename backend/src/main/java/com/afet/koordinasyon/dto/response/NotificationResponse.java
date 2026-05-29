package com.afet.koordinasyon.dto.response;

import com.afet.koordinasyon.domain.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID            id;
    private NotificationType type;
    private String          title;
    private String          message;
    private String          relatedEntityType;
    private String          relatedEntityId;
    private boolean         isRead;
    private OffsetDateTime  createdAt;
    private OffsetDateTime  readAt;
}
