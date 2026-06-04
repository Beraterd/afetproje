-- §4-9/§11 — Yakınlara acil durum mesajı: güvenli token + gönderim logu

CREATE TABLE emergency_message_tokens (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash               VARCHAR(64)  NOT NULL UNIQUE,
    user_id                  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    earthquake_notification_id UUID,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    message_type             VARCHAR(40),
    expires_at               TIMESTAMPTZ  NOT NULL,
    used_at                  TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_emt_token_hash ON emergency_message_tokens (token_hash);
CREATE INDEX idx_emt_user       ON emergency_message_tokens (user_id);
CREATE INDEX idx_emt_expires_at ON emergency_message_tokens (expires_at);

CREATE TABLE emergency_contact_message_logs (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_user_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type       VARCHAR(40)  NOT NULL,
    message_text       TEXT         NOT NULL,
    channel            VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    error_message      TEXT,
    sent_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ecml_sender    ON emergency_contact_message_logs (sender_user_id);
CREATE INDEX idx_ecml_recipient ON emergency_contact_message_logs (recipient_user_id);
