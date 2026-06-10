-- Kullanıcı profili için konum kaynağı (GPS / Network / Cell)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS location_source VARCHAR(30);
