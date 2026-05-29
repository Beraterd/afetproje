-- V34: Add coordinator location fields to districts and neighborhoods

ALTER TABLE districts
    ADD COLUMN IF NOT EXISTS coordinator_latitude  DECIMAL(10,7),
    ADD COLUMN IF NOT EXISTS coordinator_longitude DECIMAL(10,7),
    ADD COLUMN IF NOT EXISTS coordinator_location_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS coordinator_location_updated_by UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE neighborhoods
    ADD COLUMN IF NOT EXISTS coordinator_latitude  DECIMAL(10,7),
    ADD COLUMN IF NOT EXISTS coordinator_longitude DECIMAL(10,7),
    ADD COLUMN IF NOT EXISTS coordinator_location_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS coordinator_location_updated_by UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_districts_coord_lat  ON districts(coordinator_latitude) WHERE coordinator_latitude IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_nbhd_coord_lat ON neighborhoods(coordinator_latitude) WHERE coordinator_latitude IS NOT NULL;
