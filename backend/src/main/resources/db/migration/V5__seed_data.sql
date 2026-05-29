-- V5: Seed Data — Districts, Neighborhoods, Teams, Admin user

-- ============================================================
-- ACTIVE DISTRICTS (10 Istanbul pilot districts)
-- ============================================================
INSERT INTO districts (id, name, is_active, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-000000000001', 'Pendik',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000002', 'Kartal',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000003', 'Tuzla',         TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000004', 'Kadıköy',       TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000005', 'Ataşehir',      TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000006', 'Bahçelievler',  TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000007', 'Beşiktaş',      TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000008', 'Bakırköy',      TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000009', 'Fatih',         TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000010', 'Avcılar',       TRUE, now(), now());

-- ============================================================
-- SEED NEIGHBORHOODS (representative set per district)
-- ============================================================
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    -- Pendik
    ('22222222-2222-2222-2222-000000000001', 'Kurtköy',        '11111111-1111-1111-1111-000000000001', now(), now()),
    ('22222222-2222-2222-2222-000000000002', 'Pendik Merkez',  '11111111-1111-1111-1111-000000000001', now(), now()),
    ('22222222-2222-2222-2222-000000000003', 'Kaynarca',       '11111111-1111-1111-1111-000000000001', now(), now()),
    -- Kartal
    ('22222222-2222-2222-2222-000000000004', 'Kartal Merkez',  '11111111-1111-1111-1111-000000000002', now(), now()),
    ('22222222-2222-2222-2222-000000000005', 'Uğur Mumcu',     '11111111-1111-1111-1111-000000000002', now(), now()),
    -- Tuzla
    ('22222222-2222-2222-2222-000000000006', 'Tuzla Merkez',   '11111111-1111-1111-1111-000000000003', now(), now()),
    ('22222222-2222-2222-2222-000000000007', 'Aydınlı',        '11111111-1111-1111-1111-000000000003', now(), now()),
    -- Kadıköy
    ('22222222-2222-2222-2222-000000000008', 'Moda',           '11111111-1111-1111-1111-000000000004', now(), now()),
    ('22222222-2222-2222-2222-000000000009', 'Bostancı',       '11111111-1111-1111-1111-000000000004', now(), now()),
    ('22222222-2222-2222-2222-00000000000a', 'Erenköy',        '11111111-1111-1111-1111-000000000004', now(), now()),
    -- Ataşehir
    ('22222222-2222-2222-2222-00000000000b', 'Ataşehir Merkez','11111111-1111-1111-1111-000000000005', now(), now()),
    ('22222222-2222-2222-2222-00000000000c', 'Küçükbakkalköy', '11111111-1111-1111-1111-000000000005', now(), now()),
    -- Bahçelievler
    ('22222222-2222-2222-2222-00000000000d', 'Yenibosna',      '11111111-1111-1111-1111-000000000006', now(), now()),
    ('22222222-2222-2222-2222-00000000000e', 'Şirinevler',     '11111111-1111-1111-1111-000000000006', now(), now()),
    -- Beşiktaş
    ('22222222-2222-2222-2222-00000000000f', 'Levent',         '11111111-1111-1111-1111-000000000007', now(), now()),
    ('22222222-2222-2222-2222-000000000010', 'Etiler',         '11111111-1111-1111-1111-000000000007', now(), now()),
    -- Bakırköy
    ('22222222-2222-2222-2222-000000000011', 'Ataköy',         '11111111-1111-1111-1111-000000000008', now(), now()),
    ('22222222-2222-2222-2222-000000000012', 'Zeytinlik',      '11111111-1111-1111-1111-000000000008', now(), now()),
    -- Fatih
    ('22222222-2222-2222-2222-000000000013', 'Sultanahmet',    '11111111-1111-1111-1111-000000000009', now(), now()),
    ('22222222-2222-2222-2222-000000000014', 'Balat',          '11111111-1111-1111-1111-000000000009', now(), now()),
    -- Avcılar
    ('22222222-2222-2222-2222-000000000015', 'Avcılar Merkez', '11111111-1111-1111-1111-000000000010', now(), now()),
    ('22222222-2222-2222-2222-000000000016', 'Ambarlı',        '11111111-1111-1111-1111-000000000010', now(), now());

-- ============================================================
-- ASSEMBLY AREAS (sample per neighborhood)
-- ============================================================
INSERT INTO assembly_areas (neighborhood_id, name, latitude, longitude, capacity, created_at) VALUES
    ('22222222-2222-2222-2222-000000000001', 'Kurtköy Meydanı Toplanma Alanı', 40.9163, 29.1864, 1500, now()),
    ('22222222-2222-2222-2222-000000000001', 'Kurtköy Spor Sahası', 40.9120, 29.1800, 800, now()),
    ('22222222-2222-2222-2222-000000000004', 'Kartal Sahil Toplanma Alanı', 40.8913, 29.1880, 2000, now()),
    ('22222222-2222-2222-2222-000000000008', 'Moda Koşu Yolu Toplanma', 40.9812, 29.0268, 1000, now()),
    ('22222222-2222-2222-2222-000000000013', 'Sultanahmet Meydanı', 41.0054, 28.9768, 5000, now());

-- ============================================================
-- TEAMS (6 operational teams, seeded as per spec)
-- ============================================================
INSERT INTO teams (id, name, coefficient, requires_document, description, created_at, updated_at) VALUES
    ('33333333-3333-3333-3333-000000000001', 'SEARCH_RESCUE', 5.0, 'SEARCH_RESCUE_CERTIFICATE',       'Arama ve Kurtarma Ekibi — sertifika gerektirir', now(), now()),
    ('33333333-3333-3333-3333-000000000002', 'FOOD_WATER',    3.0, NULL,                             'Gıda ve Su Dağıtım Ekibi', now(), now()),
    ('33333333-3333-3333-3333-000000000003', 'LOGISTICS',     2.0, NULL,                             'Lojistik ve Malzeme Ekibi', now(), now()),
    ('33333333-3333-3333-3333-000000000004', 'EVACUATION',    4.0, NULL,                             'Tahliye Ekibi', now(), now()),
    ('33333333-3333-3333-3333-000000000005', 'COMMUNICATION', 2.0, NULL,                             'İletişim Ekibi', now(), now()),
    ('33333333-3333-3333-3333-000000000006', 'PSYCHOSOCIAL',  3.0, 'PSYCHOSOCIAL_GRADUATION_DOCUMENT','Psikososyal Destek Ekibi — mezuniyet belgesi gerektirir', now(), now());

-- ============================================================
-- ADMIN USER
-- Password: Admin@12345  (BCrypt hash)
-- Neighborhood: Kurtköy (Pendik district)
-- ============================================================
INSERT INTO users (
    id, first_name, last_name, email, email_verified,
    phone, blood_type, district_id, neighborhood_id,
    address, profession, password_hash, role, is_active, created_at, updated_at
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Sistem', 'Admin',
    'admin@afetkoordinasyon.istanbul',
    TRUE,
    '+905300000000',
    'O_POSITIVE',
    '11111111-1111-1111-1111-000000000001',
    '22222222-2222-2222-2222-000000000001',
    'Sistem Yönetim Merkezi',
    'Sistem Yöneticisi',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'ADMIN',
    TRUE,
    now(), now()
);
