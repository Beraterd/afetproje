-- =====================================================================
-- V38: Fix trg_validate_user_neighborhood to allow NULL neighborhood_id
--
-- PROBLEM
--   The original trigger (V4) does not guard against NULL neighborhood_id.
--   When neighborhood_id IS NULL the WHERE clause evaluates to
--   "WHERE id = NULL AND ..." which matches nothing, causing NOT EXISTS
--   to be TRUE and raising a spurious exception — even though NULL means
--   "no neighborhood assigned yet" which is a valid intermediate state
--   during migrations and admin operations.
--
-- NOTE: The users.neighborhood_id column is defined NOT NULL in V2, so
--   the application layer enforces a non-NULL value at insert time.
--   However, during Flyway migration transactions the trigger fires
--   before FK checks are deferred; if any migration ever needs to
--   temporarily clear a neighborhood assignment (e.g. when deleting
--   stale placeholder rows) the trigger would block it.
--   Adding a NULL guard makes the trigger robust to such operations.
-- =====================================================================

CREATE OR REPLACE FUNCTION trg_validate_user_neighborhood()
RETURNS TRIGGER AS $$
BEGIN
    -- Allow NULL neighborhood_id: column-level NOT NULL constraint
    -- already prevents NULL at the application insert/update path.
    -- During migration transactions a NULL may transiently appear
    -- while reassigning users away from deleted placeholder rows.
    IF NEW.neighborhood_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM neighborhoods
        WHERE id = NEW.neighborhood_id
          AND district_id = NEW.district_id
    ) THEN
        RAISE EXCEPTION 'neighborhood_id % does not belong to district_id %',
            NEW.neighborhood_id, NEW.district_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
