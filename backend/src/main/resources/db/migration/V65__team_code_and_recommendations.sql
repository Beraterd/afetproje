-- V65: Team code field + district association + AI team recommendation tables

-- ── 1. Team kodu ve ilçe bağlantısı ─────────────────────────────────────────

ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS team_code   VARCHAR(20),
    ADD COLUMN IF NOT EXISTS district_id UUID REFERENCES districts(id) ON DELETE SET NULL;

-- Mevcut UNIQUE kısıtlamasını kaldır (district_id ile birlikte yönetilecek)
ALTER TABLE teams DROP CONSTRAINT IF EXISTS teams_name_key;

-- Global takımlar (district_id NULL): tip başına tek kayıt
CREATE UNIQUE INDEX IF NOT EXISTS uniq_teams_name_global
    ON teams (name) WHERE district_id IS NULL;

-- İlçeye özgü takımlar: (ilçe, tip) ikilisi benzersiz
CREATE UNIQUE INDEX IF NOT EXISTS uniq_teams_name_district
    ON teams (name, district_id) WHERE district_id IS NOT NULL;

-- Ekip kodu benzersiz
CREATE UNIQUE INDEX IF NOT EXISTS uniq_teams_team_code
    ON teams (team_code) WHERE team_code IS NOT NULL;

-- Mevcut 8 global ekip için kod backfill (ilçe yoksa type-bazlı)
UPDATE teams SET team_code = CASE name::text
    WHEN 'SEARCH_RESCUE'       THEN 'SAR-1'
    WHEN 'FOOD_WATER'          THEN 'FW-1'
    WHEN 'EVACUATION'          THEN 'EVA-1'
    WHEN 'COMMUNICATION'       THEN 'COM-1'
    WHEN 'PSYCHOSOCIAL'        THEN 'PSY-1'
    WHEN 'HASAR_TESPIT_EKIBI'  THEN 'HTK-1'
    WHEN 'LOGISTICS'           THEN 'LOG-1'
    WHEN 'OTHER'               THEN 'OTH-1'
    ELSE 'GL-' || SUBSTRING(name::text, 1, 3)
END
WHERE team_code IS NULL AND district_id IS NULL;

-- ── 2. Öneri durumu enum ──────────────────────────────────────────────────────

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'recommendation_status') THEN
        CREATE TYPE recommendation_status AS ENUM (
            'DRAFT', 'APPROVED', 'REJECTED'
        );
    END IF;
END$$;

-- ── 3. Ekip öneri tablosu ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS team_recommendations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_type           team_name       NOT NULL,
    district_id         UUID            NOT NULL REFERENCES districts(id),
    neighborhood_id     UUID            REFERENCES neighborhoods(id),
    required_team_size  INTEGER         NOT NULL CHECK (required_team_size > 0),
    priority            VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    status              recommendation_status NOT NULL DEFAULT 'DRAFT',
    ai_explanation      TEXT,
    requested_by_id     UUID            NOT NULL REFERENCES users(id),
    approved_by_id      UUID            REFERENCES users(id),
    approved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- ── 4. Öneri üyesi tablosu ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS team_recommendation_members (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recommendation_id       UUID    NOT NULL REFERENCES team_recommendations(id) ON DELETE CASCADE,
    user_id                 UUID    NOT NULL REFERENCES users(id),
    score                   INTEGER NOT NULL DEFAULT 0,
    order_id                INTEGER NOT NULL DEFAULT 0,
    completed_similar_tasks INTEGER NOT NULL DEFAULT 0,
    reasons                 TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rec_member UNIQUE (recommendation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_team_recommendations_status
    ON team_recommendations (status);

CREATE INDEX IF NOT EXISTS idx_team_recommendations_district
    ON team_recommendations (district_id);

CREATE INDEX IF NOT EXISTS idx_team_recommendation_members_rec
    ON team_recommendation_members (recommendation_id);
