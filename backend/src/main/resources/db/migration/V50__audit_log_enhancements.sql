-- V50: Audit log table enhancements
-- Adds missing columns required for the full audit log system.

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS actor_name  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS metadata    JSONB,
    ADD COLUMN IF NOT EXISTS user_agent  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_system_action BOOLEAN NOT NULL DEFAULT FALSE;

-- actor_role already exists (VARCHAR 50) — no change needed

CREATE INDEX IF NOT EXISTS idx_audit_logs_is_system
    ON audit_logs (is_system_action);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_name
    ON audit_logs (actor_name);
