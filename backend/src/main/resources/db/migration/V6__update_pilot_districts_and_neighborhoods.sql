-- V6: Replace Avcılar with Beyoğlu (correct pilot district), expand neighborhoods

-- ============================================================
-- Remove Avcılar neighborhoods and district (replace with Beyoğlu)
-- ============================================================
DELETE FROM assembly_areas WHERE neighborhood_id IN (
    SELECT id FROM neighborhoods WHERE district_id = '11111111-1111-1111-1111-000000000010'
);
DELETE FROM neighborhoods WHERE district_id = '11111111-1111-1111-1111-000000000010';
DELETE FROM districts WHERE id = '11111111-1111-1111-1111-000000000010';

-- ============================================================
-- Insert Beyoğlu as the correct 10th pilot district
-- ============================================================
INSERT INTO districts (id, name, is_active, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-000000000010', 'Beyoğlu', TRUE, now(), now())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, updated_at = now();

-- ============================================================
-- Add more neighborhoods for all 10 districts
-- ============================================================

-- Pendik (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000101', 'Kavakpınar',     '11111111-1111-1111-1111-000000000001', now(), now()),
    ('22222222-2222-2222-2222-000000000102', 'Güzelyalı',      '11111111-1111-1111-1111-000000000001', now(), now()),
    ('22222222-2222-2222-2222-000000000103', 'Yenişehir',      '11111111-1111-1111-1111-000000000001', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Kartal (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000104', 'Soğanlık',       '11111111-1111-1111-1111-000000000002', now(), now()),
    ('22222222-2222-2222-2222-000000000105', 'Topselvi',       '11111111-1111-1111-1111-000000000002', now(), now()),
    ('22222222-2222-2222-2222-000000000106', 'Hürriyet',       '11111111-1111-1111-1111-000000000002', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Tuzla (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000107', 'Cami',           '11111111-1111-1111-1111-000000000003', now(), now()),
    ('22222222-2222-2222-2222-000000000108', 'İçmeler',        '11111111-1111-1111-1111-000000000003', now(), now()),
    ('22222222-2222-2222-2222-000000000109', 'Mimar Sinan',    '11111111-1111-1111-1111-000000000003', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Kadıköy (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-00000000010a', 'Fenerbahçe',     '11111111-1111-1111-1111-000000000004', now(), now()),
    ('22222222-2222-2222-2222-00000000010b', 'Suadiye',        '11111111-1111-1111-1111-000000000004', now(), now()),
    ('22222222-2222-2222-2222-00000000010c', 'Göztepe',        '11111111-1111-1111-1111-000000000004', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Ataşehir (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-00000000010d', 'Barbaros',       '11111111-1111-1111-1111-000000000005', now(), now()),
    ('22222222-2222-2222-2222-00000000010e', 'Ferhatpaşa',     '11111111-1111-1111-1111-000000000005', now(), now()),
    ('22222222-2222-2222-2222-00000000010f', 'Mevlana',        '11111111-1111-1111-1111-000000000005', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Bahçelievler (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000110', 'Cumhuriyet',     '11111111-1111-1111-1111-000000000006', now(), now()),
    ('22222222-2222-2222-2222-000000000111', 'Kocasinan',      '11111111-1111-1111-1111-000000000006', now(), now()),
    ('22222222-2222-2222-2222-000000000112', 'Zafer',          '11111111-1111-1111-1111-000000000006', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Beşiktaş (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000113', 'Ortaköy',        '11111111-1111-1111-1111-000000000007', now(), now()),
    ('22222222-2222-2222-2222-000000000114', 'Bebek',          '11111111-1111-1111-1111-000000000007', now(), now()),
    ('22222222-2222-2222-2222-000000000115', 'Arnavutköy',     '11111111-1111-1111-1111-000000000007', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Bakırköy (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000116', 'Kartaltepe',     '11111111-1111-1111-1111-000000000008', now(), now()),
    ('22222222-2222-2222-2222-000000000117', 'Osmaniye',       '11111111-1111-1111-1111-000000000008', now(), now()),
    ('22222222-2222-2222-2222-000000000118', 'Şenlik',         '11111111-1111-1111-1111-000000000008', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Fatih (additional neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000119', 'Aksaray',        '11111111-1111-1111-1111-000000000009', now(), now()),
    ('22222222-2222-2222-2222-00000000011a', 'Haseki',         '11111111-1111-1111-1111-000000000009', now(), now()),
    ('22222222-2222-2222-2222-00000000011b', 'Çarşamba',       '11111111-1111-1111-1111-000000000009', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Beyoğlu (neighborhoods)
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    ('22222222-2222-2222-2222-000000000015', 'Galata',         '11111111-1111-1111-1111-000000000010', now(), now()),
    ('22222222-2222-2222-2222-000000000016', 'Cihangir',       '11111111-1111-1111-1111-000000000010', now(), now()),
    ('22222222-2222-2222-2222-00000000011c', 'Taksim',         '11111111-1111-1111-1111-000000000010', now(), now()),
    ('22222222-2222-2222-2222-00000000011d', 'Tarlabaşı',      '11111111-1111-1111-1111-000000000010', now(), now()),
    ('22222222-2222-2222-2222-00000000011e', 'Kasımpaşa',      '11111111-1111-1111-1111-000000000010', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- ============================================================
-- Add assembly areas for Beyoğlu neighborhoods
-- ============================================================
INSERT INTO assembly_areas (neighborhood_id, name, latitude, longitude, capacity, created_at) VALUES
    ('22222222-2222-2222-2222-000000000015', 'Galata Meydanı Toplanma Alanı', 41.0257, 28.9748, 2000, now()),
    ('22222222-2222-2222-2222-00000000011c', 'Taksim Meydanı Toplanma Alanı', 41.0369, 28.9850, 5000, now()),
    ('22222222-2222-2222-2222-000000000016', 'Cihangir Parkı Toplanma Alanı', 41.0328, 28.9830, 800, now())
ON CONFLICT DO NOTHING;
