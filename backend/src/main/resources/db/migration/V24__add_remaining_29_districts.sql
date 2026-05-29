-- V24: Expand from 10 pilot districts to all 39 Istanbul districts
-- Adds 29 new districts + representative neighborhoods for all 39
-- Existing 10 districts and their data are preserved

-- ============================================================
-- 29 NEW DISTRICTS (IDs 11-39)
-- Alphabetical within region groups
-- ============================================================
INSERT INTO districts (id, name, is_active, created_at, updated_at) VALUES
    -- Anadolu yakası (yeni)
    ('11111111-1111-1111-1111-000000000011', 'Adalar',         TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000012', 'Beykoz',         TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000013', 'Çekmeköy',       TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000014', 'Maltepe',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000015', 'Sancaktepe',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000016', 'Sultanbeyli',    TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000017', 'Şile',           TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000018', 'Ümraniye',       TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000019', 'Üsküdar',        TRUE, now(), now()),
    -- Avrupa yakası (yeni)
    ('11111111-1111-1111-1111-000000000020', 'Arnavutköy',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000021', 'Avcılar',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000022', 'Bağcılar',       TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000023', 'Başakşehir',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000024', 'Bayrampaşa',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000025', 'Beylikdüzü',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000026', 'Büyükçekmece',   TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000027', 'Çatalca',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000028', 'Esenler',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000029', 'Esenyurt',       TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000030', 'Eyüpsultan',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000031', 'Gaziosmanpaşa',  TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000032', 'Güngören',       TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000033', 'Kağıthane',      TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000034', 'Küçükçekmece',   TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000035', 'Sarıyer',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000036', 'Silivri',        TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000037', 'Sultangazi',     TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000038', 'Şişli',          TRUE, now(), now()),
    ('11111111-1111-1111-1111-000000000039', 'Zeytinburnu',    TRUE, now(), now())
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- NEIGHBORHOODS FOR 29 NEW DISTRICTS
-- ============================================================

-- Adalar
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Büyükada',        '11111111-1111-1111-1111-000000000011', now(), now()),
    (gen_random_uuid(), 'Heybeliada',      '11111111-1111-1111-1111-000000000011', now(), now()),
    (gen_random_uuid(), 'Burgazada',       '11111111-1111-1111-1111-000000000011', now(), now()),
    (gen_random_uuid(), 'Kınalıada',       '11111111-1111-1111-1111-000000000011', now(), now()),
    (gen_random_uuid(), 'Sedef Adası',     '11111111-1111-1111-1111-000000000011', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Beykoz
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Beykoz Merkez',   '11111111-1111-1111-1111-000000000012', now(), now()),
    (gen_random_uuid(), 'Çubuklu',         '11111111-1111-1111-1111-000000000012', now(), now()),
    (gen_random_uuid(), 'Paşabahçe',       '11111111-1111-1111-1111-000000000012', now(), now()),
    (gen_random_uuid(), 'Kavacık',         '11111111-1111-1111-1111-000000000012', now(), now()),
    (gen_random_uuid(), 'Anadoluhisarı',   '11111111-1111-1111-1111-000000000012', now(), now()),
    (gen_random_uuid(), 'Riva',            '11111111-1111-1111-1111-000000000012', now(), now()),
    (gen_random_uuid(), 'Akbaba',          '11111111-1111-1111-1111-000000000012', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Çekmeköy
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Çekmeköy Merkez',  '11111111-1111-1111-1111-000000000013', now(), now()),
    (gen_random_uuid(), 'Hamidiye',          '11111111-1111-1111-1111-000000000013', now(), now()),
    (gen_random_uuid(), 'Alemdağ',           '11111111-1111-1111-1111-000000000013', now(), now()),
    (gen_random_uuid(), 'Taşdelen',          '11111111-1111-1111-1111-000000000013', now(), now()),
    (gen_random_uuid(), 'Reşadiye',          '11111111-1111-1111-1111-000000000013', now(), now()),
    (gen_random_uuid(), 'Sultançiftliği',    '11111111-1111-1111-1111-000000000013', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Maltepe
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Maltepe Merkez',  '11111111-1111-1111-1111-000000000014', now(), now()),
    (gen_random_uuid(), 'Bağlarbaşı',      '11111111-1111-1111-1111-000000000014', now(), now()),
    (gen_random_uuid(), 'Altayçeşme',      '11111111-1111-1111-1111-000000000014', now(), now()),
    (gen_random_uuid(), 'Cevizli',         '11111111-1111-1111-1111-000000000014', now(), now()),
    (gen_random_uuid(), 'Fındıklı',        '11111111-1111-1111-1111-000000000014', now(), now()),
    (gen_random_uuid(), 'Gülsuyu',         '11111111-1111-1111-1111-000000000014', now(), now()),
    (gen_random_uuid(), 'Küçükyalı',       '11111111-1111-1111-1111-000000000014', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Sancaktepe
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Sancaktepe Merkez', '11111111-1111-1111-1111-000000000015', now(), now()),
    (gen_random_uuid(), 'Samandıra',          '11111111-1111-1111-1111-000000000015', now(), now()),
    (gen_random_uuid(), 'Yenidoğan',          '11111111-1111-1111-1111-000000000015', now(), now()),
    (gen_random_uuid(), 'Emek',               '11111111-1111-1111-1111-000000000015', now(), now()),
    (gen_random_uuid(), 'Fatih',              '11111111-1111-1111-1111-000000000015', now(), now()),
    (gen_random_uuid(), 'Eyüp Sultan',        '11111111-1111-1111-1111-000000000015', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Sultanbeyli
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Sultanbeyli Merkez', '11111111-1111-1111-1111-000000000016', now(), now()),
    (gen_random_uuid(), 'Abdurrahmangazi',    '11111111-1111-1111-1111-000000000016', now(), now()),
    (gen_random_uuid(), 'Battalgazi',         '11111111-1111-1111-1111-000000000016', now(), now()),
    (gen_random_uuid(), 'Mehmet Akif',        '11111111-1111-1111-1111-000000000016', now(), now()),
    (gen_random_uuid(), 'Mimar Sinan',        '11111111-1111-1111-1111-000000000016', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Şile
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Şile Merkez',    '11111111-1111-1111-1111-000000000017', now(), now()),
    (gen_random_uuid(), 'Ağva',           '11111111-1111-1111-1111-000000000017', now(), now()),
    (gen_random_uuid(), 'Kalem',          '11111111-1111-1111-1111-000000000017', now(), now()),
    (gen_random_uuid(), 'Cumhuriyet',     '11111111-1111-1111-1111-000000000017', now(), now()),
    (gen_random_uuid(), 'Doğancılar',     '11111111-1111-1111-1111-000000000017', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Ümraniye
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Ümraniye Merkez', '11111111-1111-1111-1111-000000000018', now(), now()),
    (gen_random_uuid(), 'Çakmak',          '11111111-1111-1111-1111-000000000018', now(), now()),
    (gen_random_uuid(), 'Elmaşehir',       '11111111-1111-1111-1111-000000000018', now(), now()),
    (gen_random_uuid(), 'Site',            '11111111-1111-1111-1111-000000000018', now(), now()),
    (gen_random_uuid(), 'Alemdağ',         '11111111-1111-1111-1111-000000000018', now(), now()),
    (gen_random_uuid(), 'Yukarıdudullu',   '11111111-1111-1111-1111-000000000018', now(), now()),
    (gen_random_uuid(), 'Aşağıdudullu',    '11111111-1111-1111-1111-000000000018', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Üsküdar
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Üsküdar Merkez', '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Beylerbeyi',     '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Çengelköy',      '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Kuzguncuk',      '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Acıbadem',       '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Kandilli',       '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Vaniköy',        '11111111-1111-1111-1111-000000000019', now(), now()),
    (gen_random_uuid(), 'Bağlarbaşı',     '11111111-1111-1111-1111-000000000019', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Arnavutköy
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Arnavutköy Merkez', '11111111-1111-1111-1111-000000000020', now(), now()),
    (gen_random_uuid(), 'Taşoluk',           '11111111-1111-1111-1111-000000000020', now(), now()),
    (gen_random_uuid(), 'Haraçcı',           '11111111-1111-1111-1111-000000000020', now(), now()),
    (gen_random_uuid(), 'İmrahor',           '11111111-1111-1111-1111-000000000020', now(), now()),
    (gen_random_uuid(), 'Bolluca',           '11111111-1111-1111-1111-000000000020', now(), now()),
    (gen_random_uuid(), 'Hadımköy',          '11111111-1111-1111-1111-000000000020', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Avcılar
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Avcılar Merkez', '11111111-1111-1111-1111-000000000021', now(), now()),
    (gen_random_uuid(), 'Ambarlı',        '11111111-1111-1111-1111-000000000021', now(), now()),
    (gen_random_uuid(), 'Firuzköy',       '11111111-1111-1111-1111-000000000021', now(), now()),
    (gen_random_uuid(), 'Cihangir',       '11111111-1111-1111-1111-000000000021', now(), now()),
    (gen_random_uuid(), 'Denizköşkler',   '11111111-1111-1111-1111-000000000021', now(), now()),
    (gen_random_uuid(), 'Gümüşpala',      '11111111-1111-1111-1111-000000000021', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Bağcılar
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Bağcılar Merkez', '11111111-1111-1111-1111-000000000022', now(), now()),
    (gen_random_uuid(), 'Güneşli',          '11111111-1111-1111-1111-000000000022', now(), now()),
    (gen_random_uuid(), 'Kirazlı',          '11111111-1111-1111-1111-000000000022', now(), now()),
    (gen_random_uuid(), 'Yıldıztepe',       '11111111-1111-1111-1111-000000000022', now(), now()),
    (gen_random_uuid(), 'Demirkapı',        '11111111-1111-1111-1111-000000000022', now(), now()),
    (gen_random_uuid(), 'Sancaktepe',       '11111111-1111-1111-1111-000000000022', now(), now()),
    (gen_random_uuid(), 'Fevzi Çakmak',     '11111111-1111-1111-1111-000000000022', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Başakşehir
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Başakşehir Merkez', '11111111-1111-1111-1111-000000000023', now(), now()),
    (gen_random_uuid(), 'Bahçeşehir',         '11111111-1111-1111-1111-000000000023', now(), now()),
    (gen_random_uuid(), 'İkitelli',           '11111111-1111-1111-1111-000000000023', now(), now()),
    (gen_random_uuid(), 'Kayabaşı',           '11111111-1111-1111-1111-000000000023', now(), now()),
    (gen_random_uuid(), 'Güvercintepe',       '11111111-1111-1111-1111-000000000023', now(), now()),
    (gen_random_uuid(), 'Altınşehir',         '11111111-1111-1111-1111-000000000023', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Bayrampaşa
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Bayrampaşa Merkez', '11111111-1111-1111-1111-000000000024', now(), now()),
    (gen_random_uuid(), 'Kocatepe',           '11111111-1111-1111-1111-000000000024', now(), now()),
    (gen_random_uuid(), 'Altıntepsi',         '11111111-1111-1111-1111-000000000024', now(), now()),
    (gen_random_uuid(), 'Muratpaşa',          '11111111-1111-1111-1111-000000000024', now(), now()),
    (gen_random_uuid(), 'Yıldırım',           '11111111-1111-1111-1111-000000000024', now(), now()),
    (gen_random_uuid(), 'İsmetpaşa',          '11111111-1111-1111-1111-000000000024', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Beylikdüzü
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Beylikdüzü Merkez', '11111111-1111-1111-1111-000000000025', now(), now()),
    (gen_random_uuid(), 'Gürpınar',           '11111111-1111-1111-1111-000000000025', now(), now()),
    (gen_random_uuid(), 'Kavaklı',            '11111111-1111-1111-1111-000000000025', now(), now()),
    (gen_random_uuid(), 'Dereağzı',           '11111111-1111-1111-1111-000000000025', now(), now()),
    (gen_random_uuid(), 'Büyükşehir',         '11111111-1111-1111-1111-000000000025', now(), now()),
    (gen_random_uuid(), 'Cumhuriyet',         '11111111-1111-1111-1111-000000000025', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Büyükçekmece
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Büyükçekmece Merkez', '11111111-1111-1111-1111-000000000026', now(), now()),
    (gen_random_uuid(), 'Kumburgaz',            '11111111-1111-1111-1111-000000000026', now(), now()),
    (gen_random_uuid(), 'Mimarsinan',           '11111111-1111-1111-1111-000000000026', now(), now()),
    (gen_random_uuid(), 'Karaağaç',             '11111111-1111-1111-1111-000000000026', now(), now()),
    (gen_random_uuid(), 'Gürpınar',             '11111111-1111-1111-1111-000000000026', now(), now()),
    (gen_random_uuid(), 'Pınartepe',            '11111111-1111-1111-1111-000000000026', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Çatalca
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Çatalca Merkez', '11111111-1111-1111-1111-000000000027', now(), now()),
    (gen_random_uuid(), 'Çilingir',       '11111111-1111-1111-1111-000000000027', now(), now()),
    (gen_random_uuid(), 'İnceğiz',        '11111111-1111-1111-1111-000000000027', now(), now()),
    (gen_random_uuid(), 'Karacaköy',      '11111111-1111-1111-1111-000000000027', now(), now()),
    (gen_random_uuid(), 'Yaylabayır',     '11111111-1111-1111-1111-000000000027', now(), now()),
    (gen_random_uuid(), 'Ferhatpaşa',     '11111111-1111-1111-1111-000000000027', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Esenler
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Esenler Merkez', '11111111-1111-1111-1111-000000000028', now(), now()),
    (gen_random_uuid(), 'Birlik',         '11111111-1111-1111-1111-000000000028', now(), now()),
    (gen_random_uuid(), 'Turgut Reis',    '11111111-1111-1111-1111-000000000028', now(), now()),
    (gen_random_uuid(), 'Kemer',          '11111111-1111-1111-1111-000000000028', now(), now()),
    (gen_random_uuid(), 'Oruçreis',       '11111111-1111-1111-1111-000000000028', now(), now()),
    (gen_random_uuid(), 'Çiftehavuzlar',  '11111111-1111-1111-1111-000000000028', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Esenyurt
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Esenyurt Merkez', '11111111-1111-1111-1111-000000000029', now(), now()),
    (gen_random_uuid(), 'Pınar',           '11111111-1111-1111-1111-000000000029', now(), now()),
    (gen_random_uuid(), 'Yeşilkent',       '11111111-1111-1111-1111-000000000029', now(), now()),
    (gen_random_uuid(), 'Saadetdere',      '11111111-1111-1111-1111-000000000029', now(), now()),
    (gen_random_uuid(), 'Kapadokya',       '11111111-1111-1111-1111-000000000029', now(), now()),
    (gen_random_uuid(), 'Cumhuriyet',      '11111111-1111-1111-1111-000000000029', now(), now()),
    (gen_random_uuid(), 'Ardıçlı',        '11111111-1111-1111-1111-000000000029', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Eyüpsultan
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Eyüpsultan Merkez', '11111111-1111-1111-1111-000000000030', now(), now()),
    (gen_random_uuid(), 'Alibeyköy',          '11111111-1111-1111-1111-000000000030', now(), now()),
    (gen_random_uuid(), 'Göktürk',            '11111111-1111-1111-1111-000000000030', now(), now()),
    (gen_random_uuid(), 'Kemerburgaz',        '11111111-1111-1111-1111-000000000030', now(), now()),
    (gen_random_uuid(), 'İslambey',           '11111111-1111-1111-1111-000000000030', now(), now()),
    (gen_random_uuid(), 'Rami',               '11111111-1111-1111-1111-000000000030', now(), now()),
    (gen_random_uuid(), 'Topçular',           '11111111-1111-1111-1111-000000000030', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Gaziosmanpaşa
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Bağlarbaşı',       '11111111-1111-1111-1111-000000000031', now(), now()),
    (gen_random_uuid(), 'Karayolları',      '11111111-1111-1111-1111-000000000031', now(), now()),
    (gen_random_uuid(), 'Mevlana',          '11111111-1111-1111-1111-000000000031', now(), now()),
    (gen_random_uuid(), 'Sarıgöl',          '11111111-1111-1111-1111-000000000031', now(), now()),
    (gen_random_uuid(), 'Merkez',           '11111111-1111-1111-1111-000000000031', now(), now()),
    (gen_random_uuid(), 'Fevzipaşa',        '11111111-1111-1111-1111-000000000031', now(), now()),
    (gen_random_uuid(), 'Yıldıztabya',      '11111111-1111-1111-1111-000000000031', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Güngören
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Güngören Merkez', '11111111-1111-1111-1111-000000000032', now(), now()),
    (gen_random_uuid(), 'Haznedar',        '11111111-1111-1111-1111-000000000032', now(), now()),
    (gen_random_uuid(), 'Tozkoparan',      '11111111-1111-1111-1111-000000000032', now(), now()),
    (gen_random_uuid(), 'Akıncılar',       '11111111-1111-1111-1111-000000000032', now(), now()),
    (gen_random_uuid(), 'Gençosman',       '11111111-1111-1111-1111-000000000032', now(), now()),
    (gen_random_uuid(), 'Mehmet Nesih Özmen', '11111111-1111-1111-1111-000000000032', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Kağıthane
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Çeliktepe',   '11111111-1111-1111-1111-000000000033', now(), now()),
    (gen_random_uuid(), 'Hamidiye',    '11111111-1111-1111-1111-000000000033', now(), now()),
    (gen_random_uuid(), 'Merkez',      '11111111-1111-1111-1111-000000000033', now(), now()),
    (gen_random_uuid(), 'Yahya Kemal', '11111111-1111-1111-1111-000000000033', now(), now()),
    (gen_random_uuid(), 'Gültepe',     '11111111-1111-1111-1111-000000000033', now(), now()),
    (gen_random_uuid(), 'Seyrantepe',  '11111111-1111-1111-1111-000000000033', now(), now()),
    (gen_random_uuid(), 'Nurtepe',     '11111111-1111-1111-1111-000000000033', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Küçükçekmece
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Atakent',      '11111111-1111-1111-1111-000000000034', now(), now()),
    (gen_random_uuid(), 'Halkalı',      '11111111-1111-1111-1111-000000000034', now(), now()),
    (gen_random_uuid(), 'İnönü',        '11111111-1111-1111-1111-000000000034', now(), now()),
    (gen_random_uuid(), 'Cennet',       '11111111-1111-1111-1111-000000000034', now(), now()),
    (gen_random_uuid(), 'Kanarya',      '11111111-1111-1111-1111-000000000034', now(), now()),
    (gen_random_uuid(), 'Florya',       '11111111-1111-1111-1111-000000000034', now(), now()),
    (gen_random_uuid(), 'Sefaköy',      '11111111-1111-1111-1111-000000000034', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Sarıyer
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Yeniköy',        '11111111-1111-1111-1111-000000000035', now(), now()),
    (gen_random_uuid(), 'Tarabya',        '11111111-1111-1111-1111-000000000035', now(), now()),
    (gen_random_uuid(), 'Büyükdere',      '11111111-1111-1111-1111-000000000035', now(), now()),
    (gen_random_uuid(), 'Rumelihisarı',   '11111111-1111-1111-1111-000000000035', now(), now()),
    (gen_random_uuid(), 'İstinye',        '11111111-1111-1111-1111-000000000035', now(), now()),
    (gen_random_uuid(), 'Kısırkaya',      '11111111-1111-1111-1111-000000000035', now(), now()),
    (gen_random_uuid(), 'Emirgan',        '11111111-1111-1111-1111-000000000035', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Silivri
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Silivri Merkez', '11111111-1111-1111-1111-000000000036', now(), now()),
    (gen_random_uuid(), 'Gümüşyaka',     '11111111-1111-1111-1111-000000000036', now(), now()),
    (gen_random_uuid(), 'Selimpaşa',     '11111111-1111-1111-1111-000000000036', now(), now()),
    (gen_random_uuid(), 'Alibey',        '11111111-1111-1111-1111-000000000036', now(), now()),
    (gen_random_uuid(), 'Kadıköy',       '11111111-1111-1111-1111-000000000036', now(), now()),
    (gen_random_uuid(), 'Çanta',         '11111111-1111-1111-1111-000000000036', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Sultangazi
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Sultangazi Merkez', '11111111-1111-1111-1111-000000000037', now(), now()),
    (gen_random_uuid(), 'Habibler',          '11111111-1111-1111-1111-000000000037', now(), now()),
    (gen_random_uuid(), 'Cebeci',            '11111111-1111-1111-1111-000000000037', now(), now()),
    (gen_random_uuid(), 'Uğur Mumcu',        '11111111-1111-1111-1111-000000000037', now(), now()),
    (gen_random_uuid(), 'Malkoçoğlu',        '11111111-1111-1111-1111-000000000037', now(), now()),
    (gen_random_uuid(), 'Gazi',              '11111111-1111-1111-1111-000000000037', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Şişli
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Mecidiyeköy', '11111111-1111-1111-1111-000000000038', now(), now()),
    (gen_random_uuid(), 'Nişantaşı',   '11111111-1111-1111-1111-000000000038', now(), now()),
    (gen_random_uuid(), 'Okmeydanı',   '11111111-1111-1111-1111-000000000038', now(), now()),
    (gen_random_uuid(), 'Bomonti',     '11111111-1111-1111-1111-000000000038', now(), now()),
    (gen_random_uuid(), 'Feriköy',     '11111111-1111-1111-1111-000000000038', now(), now()),
    (gen_random_uuid(), 'Kurtuluş',    '11111111-1111-1111-1111-000000000038', now(), now()),
    (gen_random_uuid(), 'Teşvikiye',   '11111111-1111-1111-1111-000000000038', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;

-- Zeytinburnu
INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Zeytinburnu Merkez', '11111111-1111-1111-1111-000000000039', now(), now()),
    (gen_random_uuid(), 'Kazlıçeşme',         '11111111-1111-1111-1111-000000000039', now(), now()),
    (gen_random_uuid(), 'Veliefendi',          '11111111-1111-1111-1111-000000000039', now(), now()),
    (gen_random_uuid(), 'Çırpıcı',            '11111111-1111-1111-1111-000000000039', now(), now()),
    (gen_random_uuid(), 'Nuripaşa',           '11111111-1111-1111-1111-000000000039', now(), now()),
    (gen_random_uuid(), 'Seyitnizam',         '11111111-1111-1111-1111-000000000039', now(), now())
ON CONFLICT (name, district_id) DO NOTHING;
