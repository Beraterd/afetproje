-- =====================================================================
-- V16: Hasar tespiti kayıtlarına konum doğrulama alanları eklendi
-- =====================================================================

ALTER TABLE damage_assessments
    ADD COLUMN IF NOT EXISTS street_name       VARCHAR(255),
    ADD COLUMN IF NOT EXISTS building_no       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS location_source   VARCHAR(50)  NOT NULL DEFAULT 'NEIGHBORHOOD_CENTER',
    ADD COLUMN IF NOT EXISTS location_verified BOOLEAN      NOT NULL DEFAULT FALSE;
