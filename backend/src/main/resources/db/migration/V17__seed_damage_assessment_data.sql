-- =====================================================================
-- V17: Seed damage assessment test data with GPS coordinates
-- Covers key neighborhoods across multiple pilot districts
-- reported_by: admin user (00000000-0000-0000-0000-000000000001)
-- =====================================================================

INSERT INTO damage_assessments (
    id, district_id, neighborhood_id, address,
    latitude, longitude,
    location_source, location_verified,
    damage_level, collapse_risk, emergency_evacuation_needed,
    casualties_suspected, blocked_road, gas_leak_risk,
    note, verification_status,
    reported_by_user_id, created_at, updated_at
) VALUES

-- =====================================================================
-- Kadıköy — Moda
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000004',
 '22222222-2222-2222-2222-000000000008',
 'Moda Caddesi No:24 Kadıköy',
 40.9832, 29.0263,
 'MAP_SELECTED', TRUE,
 'HEAVY', TRUE, TRUE, FALSE, FALSE, TRUE,
 'Bodrum katı tamamen çökmüş, gaz kokusu tespit edildi.',
 'FIELD_VERIFIED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000004',
 '22222222-2222-2222-2222-000000000008',
 'Moda Caddesi No:37 Kadıköy',
 40.9818, 29.0245,
 'MAP_SELECTED', TRUE,
 'MODERATE', FALSE, FALSE, FALSE, FALSE, FALSE,
 'Dış cephe çatlakları mevcut, yapısal hasar orta düzeyde.',
 'COORDINATOR_APPROVED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000004',
 '22222222-2222-2222-2222-000000000008',
 'Mühürdar Caddesi No:9 Kadıköy',
 40.9845, 29.0280,
 'MAP_SELECTED', TRUE,
 'LIGHT', FALSE, FALSE, FALSE, FALSE, FALSE,
 'Hafif çatlaklar, bina kullanılabilir durumda.',
 'PENDING',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Kadıköy — Bostancı
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000004',
 '22222222-2222-2222-2222-000000000009',
 'Bostancı Bağdat Caddesi No:142',
 40.9630, 29.0840,
 'MAP_SELECTED', TRUE,
 'COLLAPSED', TRUE, TRUE, TRUE, TRUE, FALSE,
 'Bina tamamen yıkılmış. Enkaz altında kayıp şüphesi var.',
 'COORDINATOR_APPROVED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000004',
 '22222222-2222-2222-2222-000000000009',
 'İstasyon Caddesi No:55 Bostancı',
 40.9615, 29.0825,
 'MAP_SELECTED', TRUE,
 'HEAVY', TRUE, FALSE, FALSE, TRUE, TRUE,
 'Zemin kat tahrip olmuş, yol kapanmış.',
 'FIELD_VERIFIED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Fatih — Sultanahmet
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000009',
 '22222222-2222-2222-2222-000000000013',
 'Sultanahmet Meydanı Sokak No:3',
 41.0055, 28.9768,
 'MAP_SELECTED', TRUE,
 'MODERATE', FALSE, TRUE, FALSE, FALSE, FALSE,
 'Tarihi yapı, restorasyon gerektiriyor. Tahliye önerildi.',
 'NEEDS_REVIEW',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000009',
 '22222222-2222-2222-2222-000000000013',
 'Divan Yolu Caddesi No:17 Fatih',
 41.0062, 28.9748,
 'MAP_SELECTED', TRUE,
 'LIGHT', FALSE, FALSE, FALSE, FALSE, FALSE,
 NULL,
 'PENDING',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Fatih — Balat
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000009',
 '22222222-2222-2222-2222-000000000014',
 'Vodina Caddesi No:21 Balat',
 41.0275, 28.9462,
 'MAP_SELECTED', TRUE,
 'HEAVY', TRUE, TRUE, FALSE, FALSE, FALSE,
 'Ahşap yapı ağır hasar görmüş.',
 'FIELD_VERIFIED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Beşiktaş — Levent
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000007',
 '22222222-2222-2222-2222-00000000000f',
 'Levent Caddesi No:44 Beşiktaş',
 41.0820, 29.0108,
 'MAP_SELECTED', TRUE,
 'MODERATE', FALSE, FALSE, FALSE, FALSE, FALSE,
 'Asma kat çökmüş, bina boşaltıldı.',
 'COORDINATOR_APPROVED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000007',
 '22222222-2222-2222-2222-00000000000f',
 'Nispetiye Caddesi No:8 Levent',
 41.0835, 29.0125,
 'MAP_SELECTED', TRUE,
 'LIGHT', FALSE, FALSE, FALSE, FALSE, FALSE,
 NULL,
 'PENDING',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Bakırköy — Ataköy
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000008',
 '22222222-2222-2222-2222-000000000011',
 'Ataköy 9-10 Kısım No:32',
 40.9675, 28.8780,
 'MAP_SELECTED', TRUE,
 'COLLAPSED', TRUE, TRUE, TRUE, TRUE, TRUE,
 'Bina tamamen yıkılmış. Acil müdahale gerekli. Gaz sızıntısı ve kayıp şüphesi var.',
 'COORDINATOR_APPROVED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000008',
 '22222222-2222-2222-2222-000000000011',
 'Ataköy 5. Kısım No:14 Bakırköy',
 40.9690, 28.8762,
 'MAP_SELECTED', TRUE,
 'HEAVY', TRUE, FALSE, FALSE, FALSE, FALSE,
 'Ağır yapısal hasar, bina boşaltıldı.',
 'FIELD_VERIFIED',
 '00000000-0000-0000-0000-000000000001', now(), now()),

(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000008',
 '22222222-2222-2222-2222-000000000011',
 'Sahil Yolu Caddesi No:7 Ataköy',
 40.9660, 28.8795,
 'MAP_SELECTED', TRUE,
 'MODERATE', FALSE, FALSE, FALSE, FALSE, FALSE,
 NULL,
 'PENDING',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Pendik — Kurtköy
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000001',
 '22222222-2222-2222-2222-000000000001',
 'Kurtköy Merkez Sokak No:5 Pendik',
 40.9163, 29.1864,
 'MAP_SELECTED', TRUE,
 'MODERATE', FALSE, TRUE, FALSE, FALSE, FALSE,
 'Tahliye gerekli, bina risk altında.',
 'PENDING',
 '00000000-0000-0000-0000-000000000001', now(), now()),

-- =====================================================================
-- Ataşehir — Küçükbakkalköy
-- =====================================================================
(gen_random_uuid(),
 '11111111-1111-1111-1111-000000000005',
 '22222222-2222-2222-2222-00000000000c',
 'Küçükbakkalköy Caddesi No:18 Ataşehir',
 40.9855, 29.1048,
 'MAP_SELECTED', TRUE,
 'HEAVY', TRUE, FALSE, FALSE, FALSE, FALSE,
 'Taşıyıcı kolonlar hasar görmüş.',
 'FIELD_VERIFIED',
 '00000000-0000-0000-0000-000000000001', now(), now());
