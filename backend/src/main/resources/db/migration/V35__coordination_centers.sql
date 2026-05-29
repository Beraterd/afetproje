-- V35: District and Neighborhood Coordination Centers
-- Each district/neighborhood can have exactly one operational coordination center.

CREATE TABLE IF NOT EXISTS district_coordination_centers (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id       UUID         NOT NULL UNIQUE REFERENCES districts(id) ON DELETE CASCADE,
    address           TEXT         NOT NULL,
    street_name       VARCHAR(255),
    building_no       VARCHAR(50),
    latitude          DECIMAL(10,7) NOT NULL,
    longitude         DECIMAL(10,7) NOT NULL,
    location_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by        UUID         REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS neighborhood_coordination_centers (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    neighborhood_id   UUID         NOT NULL UNIQUE REFERENCES neighborhoods(id) ON DELETE CASCADE,
    address           TEXT         NOT NULL,
    street_name       VARCHAR(255),
    building_no       VARCHAR(50),
    latitude          DECIMAL(10,7) NOT NULL,
    longitude         DECIMAL(10,7) NOT NULL,
    location_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by        UUID         REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_dcc_district_id  ON district_coordination_centers(district_id);
CREATE INDEX IF NOT EXISTS idx_ncc_neighborhood_id ON neighborhood_coordination_centers(neighborhood_id);
