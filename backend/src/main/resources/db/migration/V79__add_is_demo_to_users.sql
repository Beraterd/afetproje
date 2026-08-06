-- Marks accounts used by the read-only "Admin Demo Modu" visitor flow.
-- Demo accounts are excluded from normal password login (see AuthService)
-- and are blocked from all write requests (see DemoModeWriteGuardFilter).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_demo BOOLEAN NOT NULL DEFAULT FALSE;
