-- =====================================================================
-- V13: Yakınlarım + Acil Durum Mesajları tabloları
-- =====================================================================

-- Yakın kişi ilişkileri
CREATE TABLE emergency_contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_emergency_contact UNIQUE (owner_user_id, contact_user_id)
);

CREATE INDEX idx_ec_owner   ON emergency_contacts(owner_user_id);
CREATE INDEX idx_ec_contact ON emergency_contacts(contact_user_id);

-- Gönderilen acil durum mesajı kaydı
CREATE TABLE emergency_status_messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_user_id   UUID NOT NULL REFERENCES users(id),
    simulation_id    UUID REFERENCES earthquake_simulations(id),
    template_key     VARCHAR(100) NOT NULL,
    message_text     TEXT         NOT NULL,
    channel          VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    recipient_count  INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_esm_sender     ON emergency_status_messages(sender_user_id);
CREATE INDEX idx_esm_simulation ON emergency_status_messages(simulation_id);

-- Alıcı bazlı log
CREATE TABLE emergency_status_message_logs (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emergency_status_message_id  UUID         NOT NULL REFERENCES emergency_status_messages(id) ON DELETE CASCADE,
    recipient_user_id            UUID         NOT NULL REFERENCES users(id),
    recipient_email              VARCHAR(255) NOT NULL,
    channel                      VARCHAR(20)  NOT NULL DEFAULT 'EMAIL',
    status                       VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    error_message                TEXT,
    sent_at                      TIMESTAMPTZ,
    created_at                   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_esml_message ON emergency_status_message_logs(emergency_status_message_id);
