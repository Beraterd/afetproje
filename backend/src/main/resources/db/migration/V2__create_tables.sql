-- V2: Create all tables (in dependency order)
-- Circular FKs (district.coordinator_id, neighborhood.coordinator_id) added deferred at end.

-- ============================================================
-- DISTRICTS (coordinator FK added later to avoid circular dep)
-- ============================================================
CREATE TABLE districts (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(100) NOT NULL UNIQUE,
    geojson_polygon       JSONB,
    risk_score            NUMERIC(10,4) NOT NULL DEFAULT 0,
    risk_score_updated_at TIMESTAMPTZ,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- NEIGHBORHOODS (coordinator FK added later)
-- ============================================================
CREATE TABLE neighborhoods (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(150)  NOT NULL,
    district_id           UUID          NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    geojson_polygon       JSONB,
    risk_score            NUMERIC(10,4) NOT NULL DEFAULT 0,
    risk_score_updated_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_neighborhood_name_district UNIQUE (name, district_id)
);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    pending_email   VARCHAR(255),
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    blood_type      blood_type   NOT NULL,
    district_id     UUID         NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    neighborhood_id UUID         NOT NULL REFERENCES neighborhoods(id) ON DELETE RESTRICT,
    address         TEXT         NOT NULL,
    profession      VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role    NOT NULL DEFAULT 'VOLUNTEER',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- ADD DEFERRED COORDINATOR FKs
-- ============================================================
ALTER TABLE districts
    ADD COLUMN coordinator_id UUID REFERENCES users(id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE neighborhoods
    ADD COLUMN coordinator_id UUID REFERENCES users(id) ON DELETE SET NULL DEFERRABLE INITIALLY DEFERRED;

-- ============================================================
-- ASSEMBLY AREAS
-- ============================================================
CREATE TABLE assembly_areas (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id UUID           NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    name            VARCHAR(200)   NOT NULL,
    latitude        DECIMAL(10,7)  NOT NULL,
    longitude       DECIMAL(10,7)  NOT NULL,
    capacity        INTEGER,
    description     TEXT,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAMS
-- ============================================================
CREATE TABLE teams (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name              team_name     NOT NULL UNIQUE,
    coefficient       NUMERIC(4,2)  NOT NULL,
    requires_document document_type,
    description       TEXT,
    leader_id         UUID          REFERENCES users(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ============================================================
-- TEAM MEMBERSHIPS
-- ============================================================
CREATE TABLE team_memberships (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    team_id   UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_team_membership UNIQUE (user_id, team_id)
);

-- ============================================================
-- EVENTS
-- ============================================================
CREATE TABLE events (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(300)   NOT NULL,
    description      TEXT,
    neighborhood_id  UUID           NOT NULL REFERENCES neighborhoods(id) ON DELETE RESTRICT,
    team_id          UUID           NOT NULL REFERENCES teams(id) ON DELETE RESTRICT,
    created_by       UUID           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status           event_status   NOT NULL DEFAULT 'OPEN',
    urgency          SMALLINT       NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    required_people  INTEGER        NOT NULL CHECK (required_people > 0),
    risk_score       NUMERIC(10,4)  NOT NULL DEFAULT 0,
    latitude         DECIMAL(10,7),
    longitude        DECIMAL(10,7),
    starts_at        TIMESTAMPTZ,
    ends_at          TIMESTAMPTZ,
    closed_at        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_event_dates CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at >= starts_at)
);

-- ============================================================
-- EVENT VOLUNTEERS
-- ============================================================
CREATE TABLE event_volunteers (
    id        UUID                   PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id  UUID                   NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    user_id   UUID                   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ            NOT NULL DEFAULT now(),
    left_at   TIMESTAMPTZ,
    status    event_volunteer_status NOT NULL DEFAULT 'ASSIGNED',
    CONSTRAINT uq_event_volunteer UNIQUE (event_id, user_id)
);

-- ============================================================
-- DOCUMENTS
-- ============================================================
CREATE TABLE documents (
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type    document_type   NOT NULL,
    status           document_status NOT NULL DEFAULT 'PENDING',
    storage_key      VARCHAR(500)    NOT NULL,
    file_name        VARCHAR(300)    NOT NULL,
    file_size_bytes  BIGINT          NOT NULL,
    mime_type        VARCHAR(100)    NOT NULL,
    rejection_reason TEXT,
    reviewed_by      UUID            REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_rejection_reason CHECK (status != 'REJECTED' OR rejection_reason IS NOT NULL)
);

-- ============================================================
-- EARTHQUAKE SIMULATIONS
-- ============================================================
CREATE TABLE earthquake_simulations (
    id           UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id  UUID                    NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    magnitude    DECIMAL(3,1)            NOT NULL CHECK (magnitude > 0 AND magnitude <= 10),
    created_by   UUID                    NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes        TEXT,
    triggered_at TIMESTAMPTZ             NOT NULL DEFAULT now(),
    email_status simulation_email_status NOT NULL DEFAULT 'QUEUED',
    created_at   TIMESTAMPTZ             NOT NULL DEFAULT now()
);

-- ============================================================
-- SIMULATION NOTIFICATION LOGS
-- ============================================================
CREATE TABLE simulation_notification_logs (
    id            UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    simulation_id UUID                NOT NULL REFERENCES earthquake_simulations(id) ON DELETE CASCADE,
    user_id       UUID                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_address VARCHAR(255)        NOT NULL,
    status        notification_status NOT NULL DEFAULT 'QUEUED',
    sent_at       TIMESTAMPTZ,
    retry_count   SMALLINT            NOT NULL DEFAULT 0,
    last_error    TEXT,
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ         NOT NULL DEFAULT now()
);

-- ============================================================
-- AUDIT LOGS
-- ============================================================
CREATE TABLE audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID         REFERENCES users(id) ON DELETE SET NULL,
    actor_role  VARCHAR(50),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   UUID,
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- EMAIL VERIFICATION TOKENS
-- ============================================================
CREATE TABLE email_verification_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(128) NOT NULL UNIQUE,
    new_email   VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    user_agent  TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
