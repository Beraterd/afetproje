-- =====================================================================
-- V28: Demo/test seed data + Kağıthane boundary fix
-- All UUIDs are deterministic — re-runnable with ON CONFLICT DO NOTHING
-- Password for demo users: Admin@12345
-- =====================================================================

-- =====================================================================
-- 1. KAĞITHANE FIX
-- Clear the district-level polygon so the map shows a circle indicator
-- instead of a potentially bad/overlapping polygon from V27.
-- Neighborhood-level polygons (from V27) are left intact.
-- =====================================================================
UPDATE districts
   SET geojson_polygon = NULL, updated_at = now()
 WHERE id = '11111111-1111-1111-1111-000000000033';  -- Kağıthane

-- Ensure Kağıthane has fixed-UUID neighborhoods so seed events can reference them.
-- Delete only if the neighborhoods don't yet have any dependent data.
-- We use INSERT ... ON CONFLICT to avoid duplicates; random-UUID rows from V24 stay.
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at)
VALUES
    ('55550001-0000-0000-0000-000000000001', 'Çeliktepe',      '11111111-1111-1111-1111-000000000033', now(), now()),
    ('55550001-0000-0000-0000-000000000002', 'Hamidiye',        '11111111-1111-1111-1111-000000000033', now(), now()),
    ('55550001-0000-0000-0000-000000000003', 'Çağlayan',        '11111111-1111-1111-1111-000000000033', now(), now()),
    ('55550001-0000-0000-0000-000000000004', 'Gültepe',         '11111111-1111-1111-1111-000000000033', now(), now()),
    ('55550001-0000-0000-0000-000000000005', 'Harmantepe',      '11111111-1111-1111-1111-000000000033', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- =====================================================================
-- 2. DEMO USERS
-- Password hash = Admin@12345 (BCrypt $2a$12$...)
-- =====================================================================
INSERT INTO users (
    id, first_name, last_name, email, email_verified,
    phone, blood_type, district_id, neighborhood_id,
    address, profession, password_hash, role, is_active, created_at, updated_at
) VALUES

-- İlçe Koordinatörü — Kadıköy
(
    '00000000-0000-0000-0000-000000000002',
    'Ahmet', 'Yılmaz',
    'ahmet.yilmaz@afetkoordinasyon.istanbul',
    TRUE,
    '+905301111111',
    'A_POSITIVE',
    '11111111-1111-1111-1111-000000000004',  -- Kadıköy
    '22222222-2222-2222-2222-000000000008',  -- Moda
    'Moda Caddesi No:10 Kadıköy',
    'Afet Koordinatörü',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'DISTRICT_COORDINATOR',
    TRUE, now(), now()
),

-- Mahalle Koordinatörü — Beşiktaş/Levent
(
    '00000000-0000-0000-0000-000000000003',
    'Fatma', 'Demir',
    'fatma.demir@afetkoordinasyon.istanbul',
    TRUE,
    '+905302222222',
    'B_POSITIVE',
    '11111111-1111-1111-1111-000000000007',  -- Beşiktaş
    '22222222-2222-2222-2222-00000000000f',  -- Levent
    'Levent Caddesi No:5 Beşiktaş',
    'Hemşire',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'NEIGHBORHOOD_COORDINATOR',
    TRUE, now(), now()
),

-- Gönüllü — Pendik/Kurtköy
(
    '00000000-0000-0000-0000-000000000004',
    'Mehmet', 'Kaya',
    'mehmet.kaya@afetkoordinasyon.istanbul',
    TRUE,
    '+905303333333',
    'O_POSITIVE',
    '11111111-1111-1111-1111-000000000001',  -- Pendik
    '22222222-2222-2222-2222-000000000001',  -- Kurtköy
    'Kurtköy Merkez No:22 Pendik',
    'İtfaiyeci',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'VOLUNTEER',
    TRUE, now(), now()
),

-- Gönüllü — Fatih/Sultanahmet
(
    '00000000-0000-0000-0000-000000000005',
    'Zeynep', 'Arslan',
    'zeynep.arslan@afetkoordinasyon.istanbul',
    TRUE,
    '+905304444444',
    'AB_POSITIVE',
    '11111111-1111-1111-1111-000000000009',  -- Fatih
    '22222222-2222-2222-2222-000000000013',  -- Sultanahmet
    'Divan Yolu No:8 Fatih',
    'Psikolog',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'VOLUNTEER',
    TRUE, now(), now()
),

-- Gönüllü — Bakırköy/Ataköy
(
    '00000000-0000-0000-0000-000000000006',
    'Ali', 'Şahin',
    'ali.sahin@afetkoordinasyon.istanbul',
    TRUE,
    '+905305555555',
    'A_NEGATIVE',
    '11111111-1111-1111-1111-000000000008',  -- Bakırköy
    '22222222-2222-2222-2222-000000000011',  -- Ataköy
    'Ataköy 5. Kısım No:3 Bakırköy',
    'İnşaat Mühendisi',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'VOLUNTEER',
    TRUE, now(), now()
),

-- Gönüllü — Kartal/Kartal Merkez (pasif)
(
    '00000000-0000-0000-0000-000000000007',
    'Elif', 'Çelik',
    'elif.celik@afetkoordinasyon.istanbul',
    FALSE,
    '+905306666666',
    'O_NEGATIVE',
    '11111111-1111-1111-1111-000000000002',  -- Kartal
    '22222222-2222-2222-2222-000000000004',  -- Kartal Merkez
    'Kartal Merkez No:17 Kartal',
    'Sosyal Hizmet Uzmanı',
    '$2a$12$P7rZdg8Y9zFhgU2nR18SquV9lXZhGAqXv8lFLvSaCZ6OhZVJLQqWa',
    'VOLUNTEER',
    FALSE, now(), now()
)

ON CONFLICT (email) DO NOTHING;

-- =====================================================================
-- 3. DEMO EVENTS (Ekip Durumu / Genel Olaylar)
-- created_by: admin user (00000000-0000-0000-0000-000000000001)
-- =====================================================================
INSERT INTO events (
    id, title, description,
    neighborhood_id, team_id, created_by,
    status, urgency, required_people, risk_score,
    latitude, longitude, starts_at, ends_at,
    created_at, updated_at
) VALUES

-- 1. Arama kurtarma — Kadıköy/Moda (ACİL)
(
    'eeeeeeee-0000-0000-0000-000000000001',
    'Moda Enkaz Arama Kurtarma',
    'Deprem hasarı: 3 katlı ahşap bina çökmüş, enkaz altında kişi şüphesi var. Arama kurtarma ekibi acilen bölgeye intikal etmeli.',
    '22222222-2222-2222-2222-000000000008',  -- Moda/Kadıköy
    '33333333-3333-3333-3333-000000000001',  -- SEARCH_RESCUE
    '00000000-0000-0000-0000-000000000001',
    'OPEN', 5, 12, 60.0,
    40.9832, 29.0263,
    now(), now() + interval '8 hours',
    now(), now()
),

-- 2. Gıda-su dağıtımı — Beşiktaş/Levent
(
    'eeeeeeee-0000-0000-0000-000000000002',
    'Levent Gıda ve Su Dağıtımı',
    'Bölgede su kesintisi nedeniyle içme suyu dağıtımı ve hazır gıda paketleri dağıtılacak. 200 hane etkilenmiş durumda.',
    '22222222-2222-2222-2222-00000000000f',  -- Levent/Beşiktaş
    '33333333-3333-3333-3333-000000000002',  -- FOOD_WATER
    '00000000-0000-0000-0000-000000000001',
    'OPEN', 3, 8, 24.0,
    41.0820, 29.0108,
    now(), now() + interval '6 hours',
    now(), now()
),

-- 3. Tahliye operasyonu — Pendik/Kurtköy
(
    'eeeeeeee-0000-0000-0000-000000000003',
    'Kurtköy Acil Tahliye Operasyonu',
    'Heyelan riski bulunan 5 apartman boşaltılıyor. Sakinlerin geçici barınaklara nakli planlanmaktadır.',
    '22222222-2222-2222-2222-000000000001',  -- Kurtköy/Pendik
    '33333333-3333-3333-3333-000000000004',  -- EVACUATION
    '00000000-0000-0000-0000-000000000001',
    'OPEN', 4, 15, 60.0,
    40.9163, 29.1864,
    now(), now() + interval '12 hours',
    now(), now()
),

-- 4. Psikososyal destek — Fatih/Sultanahmet
(
    'eeeeeeee-0000-0000-0000-000000000004',
    'Sultanahmet Psikososyal Destek Merkezi',
    'Tahliye edilen aileler için geçici barınma alanında psikososyal destek ve kriz müdahalesi hizmeti verilecektir.',
    '22222222-2222-2222-2222-000000000013',  -- Sultanahmet/Fatih
    '33333333-3333-3333-3333-000000000006',  -- PSYCHOSOCIAL
    '00000000-0000-0000-0000-000000000001',
    'OPEN', 2, 6, 18.0,
    41.0055, 28.9768,
    now(), now() + interval '24 hours',
    now(), now()
),

-- 5. İletişim kurulumu — Bakırköy/Ataköy
(
    'eeeeeeee-0000-0000-0000-000000000005',
    'Ataköy Acil İletişim Merkezi',
    'Şebekeler bölgede çökmüş. Alternatif iletişim kanalları (telsiz, uydu) kurularak koordinasyon sağlanacak.',
    '22222222-2222-2222-2222-000000000011',  -- Ataköy/Bakırköy
    '33333333-3333-3333-3333-000000000005',  -- COMMUNICATION
    '00000000-0000-0000-0000-000000000001',
    'OPEN', 3, 5, 15.0,
    40.9675, 28.8780,
    now(), now() + interval '10 hours',
    now(), now()
),

-- 6. Lojistik — Kartal/Kartal Merkez
(
    'eeeeeeee-0000-0000-0000-000000000006',
    'Kartal Lojistik Destek ve Malzeme Koordinasyonu',
    'Toplanma alanlarına battaniye, ilk yardım malzemesi ve erzak transferi. Araç koordinasyonu gerekli.',
    '22222222-2222-2222-2222-000000000004',  -- Kartal Merkez/Kartal
    '33333333-3333-3333-3333-000000000003',  -- LOGISTICS
    '00000000-0000-0000-0000-000000000001',
    'OPEN', 3, 10, 20.0,
    40.8913, 29.1880,
    now(), now() + interval '5 hours',
    now(), now()
),

-- 7. TAMAMLANDI — Kadıköy/Bostancı
(
    'eeeeeeee-0000-0000-0000-000000000007',
    'Bostancı İlk Müdahale Gıda Dağıtımı',
    'İlk müdahale kapsamında 150 aileye hazır gıda paketi teslim edildi. Operasyon başarıyla tamamlandı.',
    '22222222-2222-2222-2222-000000000009',  -- Bostancı/Kadıköy
    '33333333-3333-3333-3333-000000000002',  -- FOOD_WATER
    '00000000-0000-0000-0000-000000000001',
    'CLOSED', 4, 10, 30.0,
    40.9630, 29.0840,
    now() - interval '6 hours', now() - interval '1 hour',
    now() - interval '8 hours', now()
)

ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- 4. DEMO RESOURCE REQUESTS (Kaynak Talepleri)
-- =====================================================================
INSERT INTO resource_requests (
    id, district_id, neighborhood_id,
    request_scope, resource_type, quantity, urgency, status,
    title, description,
    created_by_user_id, created_at, updated_at
) VALUES

-- 1. Su — Kadıköy/Moda (ACİL)
(
    'aaaaaaaa-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-000000000004',  -- Kadıköy
    '22222222-2222-2222-2222-000000000008',  -- Moda
    'NEIGHBORHOOD', 'WATER', 500, 5, 'OPEN',
    'Acil İçme Suyu İhtiyacı — Moda',
    'Deprem sonrası su şebekesi hasar gördü. 500 lt pet şişe su veya su tankeri acilen gerekmektedir.',
    '00000000-0000-0000-0000-000000000002',
    now(), now()
),

-- 2. Gıda — Beşiktaş/Levent
(
    'aaaaaaaa-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-000000000007',  -- Beşiktaş
    '22222222-2222-2222-2222-00000000000f',  -- Levent
    'NEIGHBORHOOD', 'FOOD', 200, 4, 'OPEN',
    'Hazır Gıda Paketi Talebi — Levent',
    'Tahliye edilen 200 hane için 3 günlük hazır gıda paketi talep edilmektedir.',
    '00000000-0000-0000-0000-000000000003',
    now(), now()
),

-- 3. Battaniye — Pendik/Kurtköy
(
    'aaaaaaaa-0000-0000-0000-000000000003',
    '11111111-1111-1111-1111-000000000001',  -- Pendik
    '22222222-2222-2222-2222-000000000001',  -- Kurtköy
    'NEIGHBORHOOD', 'BLANKET', 300, 3, 'OPEN',
    'Battaniye ve Isıtıcı Talebi — Kurtköy',
    'Geçici barınma alanında 300 aile soğukta bekliyor. Battaniye ve seyyar ısıtıcı acilen gerekmekte.',
    '00000000-0000-0000-0000-000000000001',
    now(), now()
),

-- 4. Jeneratör — Fatih/Sultanahmet (süreçte)
(
    'aaaaaaaa-0000-0000-0000-000000000004',
    '11111111-1111-1111-1111-000000000009',  -- Fatih
    '22222222-2222-2222-2222-000000000013',  -- Sultanahmet
    'NEIGHBORHOOD', 'GENERATOR', 2, 4, 'IN_PROGRESS',
    'Jeneratör Talebi — Sultanahmet',
    'Psikososyal destek merkezi ve tıbbi konteyner için 2 adet jeneratör gereklidir. Temin süreci başlatıldı.',
    '00000000-0000-0000-0000-000000000001',
    now(), now()
),

-- 5. İş Makinesi — Bakırköy/Ataköy (ACİL)
(
    'aaaaaaaa-0000-0000-0000-000000000005',
    '11111111-1111-1111-1111-000000000008',  -- Bakırköy
    '22222222-2222-2222-2222-000000000011',  -- Ataköy
    'NEIGHBORHOOD', 'HEAVY_MACHINERY', 1, 5, 'OPEN',
    'Kepçe/Ekskavatör Talebi — Ataköy',
    'Çökmüş bina enkazının kaldırılması için 1 adet kepçe veya ekskavatör acilen gerekmektedir.',
    '00000000-0000-0000-0000-000000000001',
    now(), now()
),

-- 6. Çadır — Kartal/Kartal Merkez
(
    'aaaaaaaa-0000-0000-0000-000000000006',
    '11111111-1111-1111-1111-000000000002',  -- Kartal
    '22222222-2222-2222-2222-000000000004',  -- Kartal Merkez
    'NEIGHBORHOOD', 'TENT', 50, 3, 'OPEN',
    'Çadır Talebi — Kartal',
    'Tahliye edilen 50 aile için geçici barınma çadırı gerekmektedir. Kurulum için alan hazır.',
    '00000000-0000-0000-0000-000000000001',
    now(), now()
),

-- 7. Tıbbi malzeme — Tuzla/Tuzla Merkez
(
    'aaaaaaaa-0000-0000-0000-000000000007',
    '11111111-1111-1111-1111-000000000003',  -- Tuzla
    '22222222-2222-2222-2222-000000000006',  -- Tuzla Merkez
    'NEIGHBORHOOD', 'MEDICAL_SUPPORT', 1, 4, 'OPEN',
    'Acil Tıbbi Malzeme ve Sağlık Ekibi — Tuzla',
    'Bölgede 12 yaralı tespit edildi. İlk yardım malzeme seti ve 1 sağlık ekibi gerekmektedir.',
    '00000000-0000-0000-0000-000000000001',
    now(), now()
),

-- 8. Su — Ataşehir/Küçükbakkalköy (TAMAMLANDI)
(
    'aaaaaaaa-0000-0000-0000-000000000008',
    '11111111-1111-1111-1111-000000000005',  -- Ataşehir
    '22222222-2222-2222-2222-00000000000c',  -- Küçükbakkalköy
    'NEIGHBORHOOD', 'WATER', 200, 4, 'FULFILLED',
    'İçme Suyu — Küçükbakkalköy',
    'Su tankeri ile 200 lt içme suyu teslim edildi. Talep karşılandı.',
    '00000000-0000-0000-0000-000000000001',
    now() - interval '12 hours', now()
)

ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- 5. TEAM MEMBERSHIPS (demo kullanıcıları ekiplere ata)
-- =====================================================================
INSERT INTO team_memberships (user_id, team_id, joined_at, is_active)
VALUES
    -- Fatma Demir (NEIGHBORHOOD_COORDINATOR) → FOOD_WATER
    ('00000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-000000000002', now(), TRUE),
    -- Mehmet Kaya (VOLUNTEER) → SEARCH_RESCUE
    ('00000000-0000-0000-0000-000000000004', '33333333-3333-3333-3333-000000000001', now(), TRUE),
    -- Zeynep Arslan (VOLUNTEER) → PSYCHOSOCIAL
    ('00000000-0000-0000-0000-000000000005', '33333333-3333-3333-3333-000000000006', now(), TRUE),
    -- Ali Şahin (VOLUNTEER) → EVACUATION
    ('00000000-0000-0000-0000-000000000006', '33333333-3333-3333-3333-000000000004', now(), TRUE)
ON CONFLICT (user_id, team_id) DO NOTHING;
