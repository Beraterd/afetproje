-- V66: Event assignment invitation system

-- ── 1. Davet durumu enum ──────────────────────────────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'event_invitation_status') THEN
        CREATE TYPE event_invitation_status AS ENUM (
            'INVITED', 'ACCEPTED', 'DECLINED', 'CANCELLED'
        );
    END IF;
END$$;

-- ── 2. team_recommendations tablosuna event_id ekle ───────────────────────────

ALTER TABLE team_recommendations
    ADD COLUMN IF NOT EXISTS event_id UUID REFERENCES events(id) ON DELETE SET NULL;

-- ── 3. team_recommendation_members tablosuna yeni alanlar ────────────────────

ALTER TABLE team_recommendation_members
    ADD COLUMN IF NOT EXISTS proximity_level      VARCHAR(30),
    ADD COLUMN IF NOT EXISTS availability         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS previous_similar_task TEXT;

-- ── 4. Etkinlik atama tablosu ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS event_assignments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id),
    invited_by_id       UUID REFERENCES users(id),
    status              event_invitation_status NOT NULL DEFAULT 'INVITED',
    invitation_token    VARCHAR(128) UNIQUE NOT NULL,
    token_expires_at    TIMESTAMPTZ NOT NULL,
    recommendation_id   UUID REFERENCES team_recommendations(id) ON DELETE SET NULL,
    invited_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at         TIMESTAMPTZ,
    declined_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_assignment UNIQUE (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_assignments_event       ON event_assignments (event_id);
CREATE INDEX IF NOT EXISTS idx_event_assignments_user        ON event_assignments (user_id);
CREATE INDEX IF NOT EXISTS idx_event_assignments_token       ON event_assignments (invitation_token);
CREATE INDEX IF NOT EXISTS idx_event_assignments_status      ON event_assignments (status);
CREATE INDEX IF NOT EXISTS idx_team_recommendations_event    ON team_recommendations (event_id);
