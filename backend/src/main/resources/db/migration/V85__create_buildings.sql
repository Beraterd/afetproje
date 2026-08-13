-- V85: Real building footprints (pilot: Pendik, source: OpenStreetMap).
-- No PostGIS in this deployment — geometry is stored as GeoJSON in jsonb, matching the
-- existing districts/neighborhoods.geojson_polygon pattern. bbox_* columns are precomputed
-- at import time so viewport bbox queries can be a plain indexed range scan.
--
-- pg_trgm is a standard Postgres contrib extension (ships with postgres:16-alpine, unlike
-- PostGIS which requires a separate image) — used only for fast address-search ILIKE matching.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE buildings (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    source              VARCHAR(30)   NOT NULL,
    external_id         VARCHAR(64)   NOT NULL,
    district_id         UUID          NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    neighborhood_id     UUID          NOT NULL REFERENCES neighborhoods(id) ON DELETE RESTRICT,
    street_name         VARCHAR(255),
    building_number     VARCHAR(50),
    display_address     TEXT,
    latitude            NUMERIC(10,7) NOT NULL,
    longitude           NUMERIC(10,7) NOT NULL,
    geometry            JSONB         NOT NULL,
    height_m            NUMERIC(6,2),
    levels              INTEGER,
    estimated_height_m  NUMERIC(6,2)  NOT NULL,
    height_source       VARCHAR(20)   NOT NULL,
    building_type       VARCHAR(100),
    name                VARCHAR(255),
    bbox_min_lat        NUMERIC(10,7) NOT NULL,
    bbox_max_lat        NUMERIC(10,7) NOT NULL,
    bbox_min_lon        NUMERIC(10,7) NOT NULL,
    bbox_max_lon        NUMERIC(10,7) NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_buildings_source_external_id UNIQUE (source, external_id)
);

CREATE INDEX idx_buildings_district_id     ON buildings(district_id);
CREATE INDEX idx_buildings_neighborhood_id ON buildings(neighborhood_id);

-- Rectangle-overlap bbox queries without PostGIS: district_id narrows the scan immediately,
-- the four bound columns let Postgres range-scan "does this building's bbox intersect the
-- viewport bbox".
CREATE INDEX idx_buildings_bbox ON buildings (district_id, bbox_min_lat, bbox_max_lat, bbox_min_lon, bbox_max_lon);

-- Address search (ILIKE '%q%') support.
CREATE INDEX idx_buildings_display_address_trgm ON buildings USING gin (display_address gin_trgm_ops);
