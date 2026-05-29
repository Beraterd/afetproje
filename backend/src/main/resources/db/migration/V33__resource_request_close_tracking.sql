-- V33: Make resource_requests.title nullable, add closed_by and closed_at tracking

ALTER TABLE resource_requests
    ALTER COLUMN title DROP NOT NULL;

ALTER TABLE resource_requests
    ADD COLUMN IF NOT EXISTS closed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

-- Backfill closed_at from resolved_at for existing fulfilled/cancelled records
UPDATE resource_requests
SET closed_at = resolved_at
WHERE status IN ('FULFILLED', 'CANCELLED') AND resolved_at IS NOT NULL AND closed_at IS NULL;
