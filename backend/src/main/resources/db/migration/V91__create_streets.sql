-- V91: Named street/road segments for neighborhood-scoped street search & map fly-to.
-- Source of truth: OpenStreetMap named highway=* ways (scripts/fetch_district_streets.py),
-- clipped to this project's own neighborhood polygons (not a fresh OSM boundary fetch — reuses
-- the DB's already-correct/audited neighborhood geometry so street->neighborhood assignment is
-- guaranteed spatially consistent with everything else the app already draws).
--
-- One row = one LOGICAL street within ONE neighborhood (fetch_district_streets.py merges every
-- raw OSM way sharing the same `name` tag within the same neighborhood into a single
-- MultiLineString row before writing the ndjson — StreetImportService does a plain 1:1 import,
-- no merge logic in Java, same division of labor as buildings). A street that physically crosses
-- several neighborhoods legitimately gets one row PER neighborhood it passes through, each with
-- geometry clipped to that neighborhood's polygon — this is what makes "search scoped to the
-- selected neighborhood" a plain districtId+neighborhoodId filter with no spatial query at
-- search time.
--
-- No PostGIS in this deployment — geometry stored as GeoJSON in jsonb, same pattern as
-- districts/neighborhoods/buildings.geojson_polygon/geometry.
CREATE TABLE streets (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    source              VARCHAR(30)   NOT NULL,
    external_id         VARCHAR(64)   NOT NULL,
    district_id         UUID          NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    neighborhood_id     UUID          NOT NULL REFERENCES neighborhoods(id) ON DELETE RESTRICT,
    name                VARCHAR(255)  NOT NULL,
    normalized_name     TEXT          NOT NULL,
    road_type           VARCHAR(30),
    geometry            JSONB         NOT NULL,
    bbox_min_lat        NUMERIC(10,7) NOT NULL,
    bbox_max_lat        NUMERIC(10,7) NOT NULL,
    bbox_min_lon        NUMERIC(10,7) NOT NULL,
    bbox_max_lon        NUMERIC(10,7) NOT NULL,
    center_lat          NUMERIC(10,7) NOT NULL,
    center_lon          NUMERIC(10,7) NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- Identity for idempotent reimport: same logical street, same neighborhood. Not
    -- (source, external_id) like buildings — external_id here is just the first contributing
    -- OSM way id, kept for traceability, not uniqueness (a merged multi-way street has no single
    -- natural external id).
    CONSTRAINT uq_streets_district_neighborhood_name UNIQUE (district_id, neighborhood_id, name)
);

CREATE INDEX idx_streets_neighborhood_id ON streets(neighborhood_id);

-- Autocomplete: exact/prefix/word-prefix/contains ranking all read normalized_name — prefix
-- lookups (name LIKE 'q%') use the btree, substring lookups (name LIKE '%q%') use the trigram
-- GIN. pg_trgm is already enabled (V85).
CREATE INDEX idx_streets_normalized_name_btree ON streets (neighborhood_id, normalized_name);
CREATE INDEX idx_streets_normalized_name_trgm ON streets USING gin (normalized_name gin_trgm_ops);
