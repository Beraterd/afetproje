package com.afet.koordinasyon.domain.entity;

import com.afet.koordinasyon.domain.enums.EmergencyMessageType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * §7/§11 — Yakınlara gönderilen acil durum mesajının kaydı (denetim/iz).
 * Her alıcı + kanal için ayrı satır tutulur.
 */
@Entity
@Table(name = "emergency_contact_message_logs",
        indexes = {
                @Index(name = "idx_ecml_sender", columnList = "sender_user_id"),
                @Index(name = "idx_ecml_recipient", columnList = "recipient_user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 40)
    private EmergencyMessageType messageType;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    /** Gönderim kanalı: IN_APP, EMAIL, SMS. */
    @Column(nullable = false, length = 20)
    private String channel;

    /** SENT veya FAILED. */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
