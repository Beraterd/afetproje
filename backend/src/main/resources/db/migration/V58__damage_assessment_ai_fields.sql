-- V58: Add AI pre-assessment fields to damage_assessments.
-- All columns are nullable; existing rows remain unchanged.

ALTER TABLE damage_assessments
    ADD COLUMN IF NOT EXISTS ai_comment         TEXT,
    ADD COLUMN IF NOT EXISTS ai_confidence      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ai_model           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ai_analyzed_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ai_analysis_status VARCHAR(30);
