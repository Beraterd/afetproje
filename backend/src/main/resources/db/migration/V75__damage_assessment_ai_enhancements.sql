-- AI ön değerlendirme mimarisi genişletme: öncelik kuyruğu, öneriler, risk skoru,
-- değişim hash'i ve yeniden deneme alanları
ALTER TABLE damage_assessments
    ADD COLUMN IF NOT EXISTS ai_priority      INTEGER,
    ADD COLUMN IF NOT EXISTS ai_recommendations TEXT,
    ADD COLUMN IF NOT EXISTS ai_risk_score    INTEGER,
    ADD COLUMN IF NOT EXISTS damage_hash      VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ai_retry_count   INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ai_next_retry_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_damage_assessments_ai_status
    ON damage_assessments (ai_analysis_status);

CREATE INDEX IF NOT EXISTS idx_damage_assessments_ai_next_retry
    ON damage_assessments (ai_next_retry_at)
    WHERE ai_analysis_status = 'PENDING';
