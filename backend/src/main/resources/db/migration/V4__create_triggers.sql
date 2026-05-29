-- V4: Database Triggers

-- ============================================================
-- TRIGGER: Auto-set updated_at on mutable tables
-- ============================================================
CREATE OR REPLACE FUNCTION trg_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_districts_updated_at
    BEFORE UPDATE ON districts
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_neighborhoods_updated_at
    BEFORE UPDATE ON neighborhoods
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_teams_updated_at
    BEFORE UPDATE ON teams
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_team_memberships_updated_at
    BEFORE UPDATE ON team_memberships
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_sim_notif_updated_at
    BEFORE UPDATE ON simulation_notification_logs
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

-- ============================================================
-- TRIGGER: Auto-set closed_at when event status changes to CLOSED
-- ============================================================
CREATE OR REPLACE FUNCTION trg_event_closed_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'CLOSED' AND OLD.status != 'CLOSED' THEN
        NEW.closed_at = now();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_event_closed_at_set
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION trg_event_closed_at();

-- ============================================================
-- TRIGGER: Validate user neighborhood belongs to user district
-- ============================================================
CREATE OR REPLACE FUNCTION trg_validate_user_neighborhood()
RETURNS TRIGGER AS $$
BEGIN
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

CREATE TRIGGER trg_user_neighborhood_district_check
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trg_validate_user_neighborhood();
