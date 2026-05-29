-- V19: Add google_maps_url + district_id to assembly_areas, seed additional areas

-- ============================================================
-- 1. Add new columns
-- ============================================================
ALTER TABLE assembly_areas
    ADD COLUMN IF NOT EXISTS google_maps_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS district_id UUID REFERENCES districts(id) ON DELETE SET NULL;

-- ============================================================
-- 2. Backfill district_id from neighborhoods
-- ============================================================
UPDATE assembly_areas aa
SET district_id = n.district_id
FROM neighborhoods n
WHERE aa.neighborhood_id = n.id;

-- ============================================================
-- 3. Backfill google_maps_url for existing rows (from lat/lng)
-- ============================================================
UPDATE assembly_areas
SET google_maps_url = 'https://maps.google.com/?q=' || latitude || ',' || longitude
WHERE google_maps_url IS NULL;

-- ============================================================
-- 4. Insert additional assembly areas
-- ============================================================
INSERT INTO assembly_areas (neighborhood_id, district_id, name, latitude, longitude, google_maps_url, capacity, created_at) VALUES

    -- Kurtköy (Pendik) — 2nd area
    ('22222222-2222-2222-2222-000000000001', '11111111-1111-1111-1111-000000000001',
     'Kurtköy Tren Garı Önü', 40.9145, 29.1885,
     'https://maps.google.com/?q=40.9145,29.1885', 600, now()),

    -- Bostancı (Kadıköy) — 2 new areas
    ('22222222-2222-2222-2222-000000000009', '11111111-1111-1111-1111-000000000004',
     'Bostancı Sahil Bandı Toplanma Alanı', 40.9598, 29.0803,
     'https://maps.google.com/?q=40.9598,29.0803', 2000, now()),
    ('22222222-2222-2222-2222-000000000009', '11111111-1111-1111-1111-000000000004',
     'Bostancı Metro İstasyonu Çıkışı', 40.9625, 29.0832,
     'https://maps.google.com/?q=40.9625,29.0832', 800, now()),

    -- Balat (Fatih) — 2 new areas
    ('22222222-2222-2222-2222-000000000014', '11111111-1111-1111-1111-000000000009',
     'Balat Fener Rum Okulu Önü', 41.0252, 28.9481,
     'https://maps.google.com/?q=41.0252,28.9481', 500, now()),
    ('22222222-2222-2222-2222-000000000014', '11111111-1111-1111-1111-000000000009',
     'Vodina Caddesi Boş Alan', 41.0278, 28.9462,
     'https://maps.google.com/?q=41.0278,28.9462', 300, now()),

    -- Levent (Beşiktaş) — 2 new areas
    ('22222222-2222-2222-2222-00000000000f', '11111111-1111-1111-1111-000000000007',
     'Levent Metro Çıkışı Toplanma Alanı', 41.0818, 29.0102,
     'https://maps.google.com/?q=41.0818,29.0102', 1500, now()),
    ('22222222-2222-2222-2222-00000000000f', '11111111-1111-1111-1111-000000000007',
     'Kanyon AVM Önü Açık Alan', 41.0778, 29.0099,
     'https://maps.google.com/?q=41.0778,29.0099', 1200, now()),

    -- Ataköy (Bakırköy) — 2 new areas
    ('22222222-2222-2222-2222-000000000011', '11111111-1111-1111-1111-000000000008',
     'Ataköy Sahil Parkı Toplanma Alanı', 40.9642, 28.8780,
     'https://maps.google.com/?q=40.9642,28.8780', 3000, now()),
    ('22222222-2222-2222-2222-000000000011', '11111111-1111-1111-1111-000000000008',
     'Ataköy 5. Kısım Açık Alan', 40.9690, 28.8762,
     'https://maps.google.com/?q=40.9690,28.8762', 1000, now()),

    -- Küçükbakkalköy (Ataşehir) — 2 new areas
    ('22222222-2222-2222-2222-00000000000c', '11111111-1111-1111-1111-000000000005',
     'Küçükbakkalköy Parkı Toplanma Alanı', 40.9850, 29.1042,
     'https://maps.google.com/?q=40.9850,29.1042', 700, now()),
    ('22222222-2222-2222-2222-00000000000c', '11111111-1111-1111-1111-000000000005',
     'Ataşehir Bulvarı Toplanma Noktası', 40.9865, 29.1055,
     'https://maps.google.com/?q=40.9865,29.1055', 500, now()),

    -- Moda (Kadıköy) — 2nd area
    ('22222222-2222-2222-2222-000000000008', '11111111-1111-1111-1111-000000000004',
     'Fenerbahçe Parkı Toplanma Alanı', 40.9769, 29.0318,
     'https://maps.google.com/?q=40.9769,29.0318', 1200, now()),

    -- Sultanahmet (Fatih) — 2nd area
    ('22222222-2222-2222-2222-000000000013', '11111111-1111-1111-1111-000000000009',
     'At Meydanı Parkı Toplanma Noktası', 41.0044, 28.9752,
     'https://maps.google.com/?q=41.0044,28.9752', 2000, now());
