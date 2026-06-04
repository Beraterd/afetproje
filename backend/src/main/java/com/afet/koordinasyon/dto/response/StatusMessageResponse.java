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
    /** Geriye uyumluluk: toplam başarılı teslim (e-posta + WhatsApp). */
    private int sentCount;
    /** Başarılı e-posta teslim sayısı. */
    private int emailSentCount;
    /** Başarılı WhatsApp teslim sayısı. */
    private int whatsappSentCount;
    private OffsetDateTime createdAt;
}
