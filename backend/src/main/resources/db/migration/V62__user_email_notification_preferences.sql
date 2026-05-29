-- V62: Per-user email notification preference flags.
-- Security emails (verification, password reset) are always sent regardless of preferences.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_task_notifications_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_damage_notifications_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_earthquake_notifications_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_team_notifications_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_aid_notifications_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_system_notifications_enabled      BOOLEAN NOT NULL DEFAULT TRUE;
