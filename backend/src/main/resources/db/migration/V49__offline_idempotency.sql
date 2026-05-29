-- V49: Offline idempotency — client_generated_id columns
-- Enables the frontend offline queue to re-send queued actions without creating duplicates.
-- The backend returns the existing record when the same clientGeneratedId is received again.

ALTER TABLE emergency_status_messages
    ADD COLUMN IF NOT EXISTS client_generated_id VARCHAR(36);

CREATE UNIQUE INDEX IF NOT EXISTS uq_esm_client_generated_id
    ON emergency_status_messages (client_generated_id)
    WHERE client_generated_id IS NOT NULL;

ALTER TABLE damage_assessments
    ADD COLUMN IF NOT EXISTS client_generated_id VARCHAR(36);

CREATE UNIQUE INDEX IF NOT EXISTS uq_da_client_generated_id
    ON damage_assessments (client_generated_id)
    WHERE client_generated_id IS NOT NULL;
