-- Konum izni ve acil mesaj token konum desteği

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_known_latitude              DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS last_known_longitude             DECIMAL(11, 8),
    ADD COLUMN IF NOT EXISTS last_known_location_accuracy     DECIMAL(8, 2),
    ADD COLUMN IF NOT EXISTS location_permission_status       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS last_known_location_updated_at   TIMESTAMP WITH TIME ZONE;

ALTER TABLE emergency_message_tokens
    ADD COLUMN IF NOT EXISTS latitude          DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS longitude         DECIMAL(11, 8),
    ADD COLUMN IF NOT EXISTS location_accuracy DECIMAL(8, 2),
    ADD COLUMN IF NOT EXISTS location_source   VARCHAR(30),
    ADD COLUMN IF NOT EXISTS maps_url          TEXT;
