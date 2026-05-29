-- V32: Add approval tracking fields to damage_assessments and create photos table

ALTER TABLE damage_assessments
    ADD COLUMN IF NOT EXISTS approved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS damage_assessment_photos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    damage_assessment_id UUID NOT NULL REFERENCES damage_assessments(id) ON DELETE CASCADE,
    storage_key     VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT,
    download_token  UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_dam_photos_assessment ON damage_assessment_photos(damage_assessment_id);
CREATE INDEX IF NOT EXISTS idx_dam_photos_token ON damage_assessment_photos(download_token);
