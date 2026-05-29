-- V55: Seed HASAR_TESPIT_EKIBI team + create damage_assessment_assignments table
--
-- This migration runs in a SEPARATE transaction from V54 so that the newly
-- added 'HASAR_TESPIT_EKIBI' enum value is already committed and can be used
-- in DML statements. (PostgreSQL forbids using ALTER TYPE ADD VALUE and DML
-- with the new value in the same transaction.)

-- 1. Seed HASAR_TESPIT_EKIBI team (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM teams WHERE name = 'HASAR_TESPIT_EKIBI'
    ) THEN
        INSERT INTO teams (id, name, coefficient, requires_document, created_at, updated_at)
        VALUES (gen_random_uuid(), 'HASAR_TESPIT_EKIBI', 1.0, NULL, now(), now());
    END IF;
END;
$$;

-- 2. Create damage_assessment_assignments table
CREATE TABLE IF NOT EXISTS damage_assessment_assignments (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    damage_assessment_id  UUID        NOT NULL REFERENCES damage_assessments(id) ON DELETE CASCADE,
    user_id               UUID        NOT NULL REFERENCES users(id),
    assigned_by_user_id   UUID        NOT NULL REFERENCES users(id),
    assigned_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    removed_at            TIMESTAMPTZ,
    is_active             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_daa_damage_assessment_id
    ON damage_assessment_assignments(damage_assessment_id);

CREATE INDEX IF NOT EXISTS idx_daa_user_id
    ON damage_assessment_assignments(user_id);

CREATE INDEX IF NOT EXISTS idx_daa_active
    ON damage_assessment_assignments(damage_assessment_id, is_active);
