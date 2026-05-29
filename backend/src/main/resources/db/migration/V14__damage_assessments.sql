-- =====================================================================
-- V14: Hasar Tespiti tablosu
-- =====================================================================

CREATE TABLE damage_assessments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id                 UUID         NOT NULL REFERENCES districts(id),
    neighborhood_id             UUID         NOT NULL REFERENCES neighborhoods(id),
    address                     TEXT         NOT NULL,
    latitude                    DECIMAL(10,7),
    longitude                   DECIMAL(10,7),
    building_type               VARCHAR(100),
    floor_count                 INTEGER,
    occupancy_type              VARCHAR(100),
    damage_level                VARCHAR(30)  NOT NULL DEFAULT 'UNASSESSED',
    collapse_risk               BOOLEAN      NOT NULL DEFAULT FALSE,
    emergency_evacuation_needed BOOLEAN      NOT NULL DEFAULT FALSE,
    casualties_suspected        BOOLEAN      NOT NULL DEFAULT FALSE,
    blocked_road                BOOLEAN      NOT NULL DEFAULT FALSE,
    gas_leak_risk               BOOLEAN      NOT NULL DEFAULT FALSE,
    note                        TEXT,
    verification_status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    reported_by_user_id         UUID         NOT NULL REFERENCES users(id),
    verified_by_user_id         UUID         REFERENCES users(id),
    verified_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_da_district      ON damage_assessments(district_id);
CREATE INDEX idx_da_neighborhood  ON damage_assessments(neighborhood_id);
CREATE INDEX idx_da_damage_level  ON damage_assessments(damage_level);
CREATE INDEX idx_da_verification  ON damage_assessments(verification_status);
CREATE INDEX idx_da_created_at    ON damage_assessments(created_at);
