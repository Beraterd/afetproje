-- =====================================================================
-- V68: Kısa link (short link) tablosu
-- WhatsApp/SMS gibi kanallarda uzun URL yerine /s/{code} yönlendirmesi için.
-- =====================================================================

CREATE TABLE short_links (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                 VARCHAR(16)  NOT NULL,
    target_url           VARCHAR(1000) NOT NULL,
    user_id              UUID         REFERENCES users(id) ON DELETE SET NULL,
    earthquake_event_id  UUID         REFERENCES earthquake_events(id) ON DELETE SET NULL,
    expires_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_short_link_code UNIQUE (code)
);

CREATE INDEX idx_short_link_code ON short_links(code);
