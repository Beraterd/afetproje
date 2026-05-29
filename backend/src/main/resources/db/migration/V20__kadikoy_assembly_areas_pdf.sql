-- =====================================================================
-- V20: Kadıköy assembly areas from official Kadıköy Belediyesi PDF
-- Source: https://afet.kadikoy.bel.tr/uploads/afet.kadikoy.bel.tr/20220509162901.pdf
-- 154 records across 21 neighborhoods
-- Geocoded: 6 (high-confidence known locations)
-- Needs review: 148 (coordinates require geocoding)
-- =====================================================================

-- =====================================================================
-- 1. Schema changes
-- =====================================================================
ALTER TABLE assembly_areas
    ALTER COLUMN latitude  DROP NOT NULL,
    ALTER COLUMN longitude DROP NOT NULL;

ALTER TABLE assembly_areas
    ADD COLUMN IF NOT EXISTS address        VARCHAR(300),
    ADD COLUMN IF NOT EXISTS source_name    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS source_reference VARCHAR(500),
    ADD COLUMN IF NOT EXISTS needs_review   BOOLEAN NOT NULL DEFAULT FALSE;

-- =====================================================================
-- 2. Insert all 154 records via DO block (skips missing neighborhoods)
-- =====================================================================
DO $$
DECLARE
  d_id  UUID := '11111111-1111-1111-1111-000000000004'; -- Kadıköy
  src   TEXT := 'Kadıköy Belediyesi PDF';
  sref  TEXT := 'https://afet.kadikoy.bel.tr/uploads/afet.kadikoy.bel.tr/20220509162901.pdf';
  nh    UUID;
BEGIN

  -- ================================================================
  -- 19 MAYIS (13 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = '19 Mayıs' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, '19 MAYIS PARKI',                          'Bayar Cad. / Sultan Sok.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AKASYA PARKI',                            'İnönü Cad. / Panaroma Sok.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'EKİN PARK',                               'İnönü Cad. / Mehpare Sk / Aydın Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HÜRRİYET PARKI',                          'Bayar Cad. / Sultan Sok.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HELİN PALANDÖKEN PARKI',                  'Bayar Cad. / Toktaş Sok.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KRİTON CURİ PARKI',                       'Okur Sok. / Rıfkı Bey Sok.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KUŞLUK PARKI',                            'Sultan Sok. / Millet Sok.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ZÜBEYDE HANIM PARKI',                     'Hilmipaşa Sok. / Hüseyin Ayanoğlu Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ORMEN SİTESİ PARKI',                      'Bayar Cd.',                          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'IŞIK OKULLARI',                           'Sinan Ercan Sok.',                   NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MUSTAFA NAZMİ ERSİN CAMİİ',               'Şahin Sok. / Saray Sok.',            NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KAZASKER SHELL KARŞISI YEŞİL ALAN',       'Güneşli Sok. / Kaptan Arif Sok.',    NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ŞÖHRET KURŞUNOĞLU ÖZEL MESLEK OKULU',     'Tüccarbaşı Sok.',                    NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood 19 Mayıs not found, skipping 13 records';
  END IF;

  -- ================================================================
  -- ACIBADEM (14 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Acıbadem' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'CEREN DAMAR PARKI',                        'Mustafa Bey Sok. / Defne Sok.',      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'J.ER CEMAL TÜFEKCİOĞLU PARKI',            'Onur Sok. / Çiftlik Sok.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KURUÇEŞME PARKI',                          'Acıbadem Cad. / Köftüncü Sok.',      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SOKULLU PARKI',                            'Fatih Sok. / Sokullu Sok.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÜÇGEN PARKI',                              'Atıfbey Sok. / Betül Sok.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MEHTAP BÜLBÜL PARKI',                      'Acıbadem Cad. / Defne Dok.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÖZDEMİROĞLU İMAM HATİP ORTA OKULU',       'Acıbadem Cad. / Taşköprü Cad.',      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MARMARA GÜZEL SANATLAR FAKÜLTESİ BAHÇESİ','Acıbadem Cad. / Betül Sok.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AHMET SÜT CAMİİ',                         'Faik Ali Sok. / Gömeç Sok.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HUKUKÇULAR SİTESİ ORTA BAHÇE',            'Necipbey Sok. / Nakkaş Sok.',        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AHMET SANİ GEZİCİ KIZ İMAM HATİP LİSESİ','Acıbadem Cad. / Necipbey Sok.',      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'NATİLLİUS AVM BAHÇESİ',                   'Meydan Cad. / Fatih Sok.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'TÜRK KADINLAR BİRLİĞİ PARKI',             'Taşköprü Cad.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'NATİLLİUS AVM KARŞISI İSPARK',            'Dinlenç Cad.',                       NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Acıbadem not found, skipping 14 records';
  END IF;

  -- ================================================================
  -- BOSTANCI (15 kayıt) — 1 geocoded
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Bostancı' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'MENEKŞE PARKI',                            'Emanet Sk.',                         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MUALLA SELCANOĞLU MESLEKİ VE TEKNİK ANADOLU LİSESİ', 'Prf. Dr. Kemal Akgüder Cad.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HAYRULLAH KEFOĞLU ANADOLU LİSESİ',         'Tayyareci Resmi Sok. / Bostan Tariki Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'CLUP SPORİUM',                             'Bostancı Yanyol Cad.',               NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KULOĞLU CAMİİ',                            'Vukela Cad. / Bostancı Camii Sok.',  NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ATATÜRK ORTAOKULU',                        'Ali Nihat Tarlan Cad. / Bostan Tariki Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'BOSTANCI GÖSTERİ MERKEZİ',                 'Bostanlararası Sok.',                40.9621, 29.0834, 'https://www.google.com/maps?q=40.9621,29.0834', src, sref, FALSE, now()),
      (nh, d_id, 'ESKİ BOSTANCI PAZAR YERİ',                 'Bostanlararası Sok.',                NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'LEMAN KAYA İLKOKULU',                      'Bostanlararası Sok.',                NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MELİH İSKANDİYAR İLKOKULU',               'Kocayok Cad.',                       NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'TATARAĞASI CAMİİ',                         'Ali Nihat Tarlan Cad.',              NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, '23 NİSAN ZEHRA HANIM İMAM HATİP ORTA OKULU', 'Gümüşçü Sok.',                    NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KILIÇ SPOR TESİSLERİ',                     'Bostanlararası Sok.',                NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MAT OTOMATİV',                             'Bostanlararası Sok. / Emin Ali Paşa Cad.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MİLTAŞ SPOR TESİSLERİ',                   'Havacı Binbaşı Mehmet Sok. / Yeni Gelin Sok.', NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Bostancı not found, skipping 15 records';
  END IF;

  -- ================================================================
  -- CADDEBOSTAN (4 kayıt) — PDF'de "CADDEPOSTAN" yazıyor
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Caddebostan' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'GÖZTEPE 60. YIL PARKI',                    'Hulusi Behçet Cad.',                 NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ŞEHİT ORHUN GÖKTAY İLKOKULU',              'Bilim Sok.',                         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, '50. YIL TARHAN ANADOLU LİSESİ',            'Operatör Cemil Topuzlu Cad.',        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'TARIM İL MÜDÜRLÜĞÜ',                       'Bağdat Cad.',                        NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Caddebostan not found (PDF: CADDEPOSTAN), skipping 4 records';
  END IF;

  -- ================================================================
  -- CAFERAĞA (4 kayıt) — 2 geocoded
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Caferağa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'MODA PARKI',                               'Ferit Tek Sk.',                      40.9866, 29.0243, 'https://www.google.com/maps?q=40.9866,29.0243', src, sref, FALSE, now()),
      (nh, d_id, 'ŞAİR NEFİ PARKI',                          'Şair Nefi Sk.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KADIKÖY ANADOLU LİSESİ',                   'Dr. Esat Işık Cad.',                 40.9853, 29.0267, 'https://www.google.com/maps?q=40.9853,29.0267', src, sref, FALSE, now()),
      (nh, d_id, 'FRANSIZ LİSESİ',                           'Doktor Esat Işık Cad.',              NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Caferağa not found, skipping 4 records';
  END IF;

  -- ================================================================
  -- DUMLUPINAR (4 kayıt) — 1 geocoded
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Dumlupınar' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'FENERBAJÇE SPOR TESİSLERİ',                'Yumurtacı Abdi Bey Cad. / Bahçem Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AHMET SANİ GEZİCİ ÇOK PROGRAMLI ANADOLU LİSESİ', 'Yumurtacı Abdi Bey Cad. / Pelin Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MEDENİYET ÜNİVERSİTESİ',                   'D-100 Karayolu / Yumurtarcı Abdibey Sok.', 40.9749, 29.0712, 'https://www.google.com/maps?q=40.9749,29.0712', src, sref, FALSE, now()),
      (nh, d_id, 'MEHMET BEYAZIT ANADOLU LİSESİ BAHÇESİ',    'Hızırbey Cad. / Merdivenköy Yolu Sok.', NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Dumlupınar not found, skipping 4 records';
  END IF;

  -- ================================================================
  -- EĞİTİM (6 kayıt) — 2 geocoded
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Eğitim' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'KEMAL SUNAL PARKI',                        'Hilmibey Sk. / Adilbey Sk.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SSK ARKASI PARKI',                         'Hızırbey Cd. / Mektep Sk.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ŞHT. HAKAN BAYLAN PARKI',                  'Yeşilköşk Sk.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'DAYANIŞMA PARKI',                          'Babacan Sk.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ATATÜRK FEN LİSESİ',                       'Sarayönü Cad. / Muratpaşa Cad.',     40.9986, 29.0541, 'https://www.google.com/maps?q=40.9986,29.0541', src, sref, FALSE, now()),
      (nh, d_id, 'MARMARA ÜNİVERSİTESİ',                     'Fahrettin Kerim Gökay Cad.',         40.9999, 29.0586, 'https://www.google.com/maps?q=40.9999,29.0586', src, sref, FALSE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Eğitim not found, skipping 6 records';
  END IF;

  -- ================================================================
  -- ERENKÖY (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Erenköy' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'GARDENYA ÇIKMAZI PARKI',                   'Gardenya Çıkmazı Sk.',               NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ERENKÖY KIZ LİSESİ',                       'Ömerpaşa Sok. / Rıdavanpaşa Sok.',   NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ERENKÖY GÖNÜLLÜ MERKESİ ARKASI PARK',      'Eralp Sok.',                         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ZİHNİ PAŞA ÇAMİ AVLUSU',                   'Telli Kavak Sok. / Erenköy İstasyon Cad.', NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Erenköy not found, skipping 4 records';
  END IF;

  -- ================================================================
  -- FENERBAHÇE (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Fenerbahçe' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'ÇAMLIK (IHLAMUR) PARKI',                   'Bozkır Sk. / Yeşilkır Sk.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'BEHİCE YAZGAN PARKI',                      'Fener Kalamış Cd.',                  NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'FENERYOLU HALK EĞİTİM MERKEZİ',            'Dr. Faruk Ayanoğlu Cad. / Ali Fuat Başgil Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'NURETTİN TEKSAN ORTA OKULU',                'Gazi Mehmetcik Sok.',                NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Fenerbahçe not found, skipping 4 records';
  END IF;

  -- ================================================================
  -- FENERYOLU (8 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Feneryolu' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'KUYUBAŞI PARKI',                           'F.Kerim Gökay Cd. / Kuyubaşı Sk.',   NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'FENERYOLU MUHTARLIK PARKI',                 'Dostane Sk.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, '26 MART PARKI',                            'Feneryolu Sk. / Erdoğdu Sk.',        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SANAT PARKI',                              'F.Kerim Gökay Cd. / Şehir Kahya Sk.',NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MELAHAT ŞEFİZADE İLKOKULU',                'Hamdibey Sok.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MUSTAFA AYKIN İLKOKULU',                   'Gazi Mustafapaşa Sok.',              NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ADEN TENİS KULÜBÜ VE BASKET SAHALARI',     'Fahir Açan Sok.',                    NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'TERAS CAFE ÇEVRESİ',                       'Mashar Osman Sok.',                  NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Feneryolu not found, skipping 8 records';
  END IF;

  -- ================================================================
  -- FİKİRTEPE (1 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Fikirtepe' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'TEPE CAMİİ AVLUSU',                        'Niyazi Bey Sok. / Özen Sok.',        NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Fikirtepe not found, skipping 1 record';
  END IF;

  -- ================================================================
  -- GÖZTEPE (11 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Göztepe' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'DEMOKRASİ PARKI',                          'Kortanpaşa Sk. / Ahmet Refik Sk.',   NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÖZGECAN ASLAN PARKI',                      'Yeşilçeşme Sk.',                     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'NADİRAĞA PARKI',                           'Nadirağa Sk. / Ege Sk.',             NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KARANFİL SOKAK PARKI',                     'Karanfil Sk. / Damla Sk.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÖZGÜRLÜK PARKI',                           'Mustafa Mashar Bey Sk.',             NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'İHSAN KURŞUNOĞLU ANADOLU LİSESİ',          'Tanzimat Sok.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ALİ HAYDAR ERSOY ORTA OKULU',              'Bağdat Cad.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MEHMET AYDOSLU İŞİTME ENGELLİLER ORTA OKULU', 'Tanzimat Sok. / Bestekar Ziya Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AVNİ AKYOL GÜZEL SANATLAR LİSESİ',         'Rıdvanpaşa Sk. / Ömer Paşa Sok.',   NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'İLHAMİ AHMED ÖRNEKAL İLKOKULU',            'Bağdat Cad.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'YEŞİLÇEŞME SOK. SPOR ALANI',               'Yeşilçeşme Sk.',                     NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Göztepe not found, skipping 11 records';
  END IF;

  -- ================================================================
  -- HASANPAŞA (6 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Hasanpaşa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'YENİ SALI PAZARI',                         'Uzuncayır Cad. / Mandıra Cad.',      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KADIKÖY ANADOLU İMAM HATİP LİSESİ',        'Uzuncayır Cad.',                     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'GAZHANE',                                  'Uzuncayır Cad. / Gazhane Deresi Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SÖĞÜTLÜÇEŞME TREN İSTASYONU',               'Taşköprü Cad. / Fahrettin Kerim Gökay Cad.', 40.9934, 29.0480, 'https://www.google.com/maps?q=40.9934,29.0480', src, sref, FALSE, now()),
      (nh, d_id, 'DOĞA KOLEJİ VE CAMİİ AVLUSU',              'Zeamet Sok.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KADIKÖY İMAM HATİP ORTA OKULU',            'Uzunçayır Cad.',                     NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Hasanpaşa not found, skipping 6 records';
  END IF;

  -- ================================================================
  -- KOŞUYOLU (12 kayıt) — 1 geocoded
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Koşuyolu' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'KOŞUYOLU PARKI',                           'Mühittin Üstündağ Sok. / Mehmet Akman Sok.', 41.0063, 29.0338, 'https://www.google.com/maps?q=41.0063,29.0338', src, sref, FALSE, now()),
      (nh, d_id, 'MİMOZA PARKI',                             'Mütevelli Çeşme Cad. / Cenap Şahabettin Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ŞEKER PARKI',                              'Şeker Parkı',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'YAŞAM PARKI',                              'Koşuyolu Cad. / Türkan Sok.',        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÖĞRETMENLER PARKI',                        'Görgülü Sk.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MANOLYA PARKI',                            'Şevket Kantarcı Sk.',                NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ESKİ KOŞUYOLU KALP HASTANESİ',             'Koşuyolu Cad. / Salih Omurtak Sok.',  NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'CENAP ŞEHABETTIN İLKOKULU BAHÇESİ',        'Cenap Şahabettin Sok. / Ferhat Acarkent Sok.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HALİL TÜRKKAN ORTAOKULU',                  'Görgülü Sk.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'REŞAT NURİ GÜLTEKİN ORTAOKULU',           'Salih Omurtak Sok.',                 NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SAMYELİ FUTBOL SAHASI',                    'Görgülü Sk.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AHMET TANER KIŞLALI PARKI',                'Hüseyin Ayanoğlu Sk.',               NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Koşuyolu not found, skipping 12 records';
  END IF;

  -- ================================================================
  -- KOZYATAĞI (12 kayıt) — 1 geocoded
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Kozyatağı' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'BARIŞ PARKI-1',                            'Korku Sk. / Belediye Blokları Sk.',  NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'BARIŞ PARKI-2',                            'Korkut Sk. / Fatih Devravut Sk.',    NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'FİRUZAN TOPRAK PARKI',                     'Gazi Ethem Paşa Sk. / Lemi Atlı Sk.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ILGIN PARKI',                              'Değirmen Çıkmazı Sk.',               NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SARI KANARYA PARKI',                       'Sarı Kanarya Sk. / Saniye Ermutlu Sk.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'AFET EĞİTİM VE BİLİNÇLENDİRME PARKI',     'Saniye Ermutlu Sk.',                 NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HAKKI DEĞER ORTA OKULU',                   'Gazi Ethem Paşa Sk.',                NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'İSMAİL ERDEM ORTAOKULU',                   'Değirmen Sok.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'KOZYATAĞI METRO GİRİŞ ALANI',              'E 80 Yolu',                          40.9855, 29.1027, 'https://www.google.com/maps?q=40.9855,29.1027', src, sref, FALSE, now()),
      (nh, d_id, 'MEHMET ÇAVUŞ CAMİ',                        'Şakacı Sok.',                        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ŞÜKRAN KARABELLİ İLKOKULU',                'Okul Sok.',                          NULL, NULL, NULL, src, sref, TRUE, now()),
      -- #106 (PDF sırası) Kozyatağı'ndaki kayıt buraya taşındı
      (nh, d_id, 'AHMET TANER KIŞLALI PARKI (KOZYATAĞI)',    'Hüseyin Ayanoğlu Sk.',               NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Kozyatağı not found, skipping 12 records';
  END IF;

  -- ================================================================
  -- MERDİVENKÖY (14 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Merdivenköy' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'ÖZLEM SOKAK PARKI',                        'Şair Arşi Cd. / Özlem Sk.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÇINAR PARKI',                              'Dr. Erkin Cd. / Karaman Sk.',        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'EYLÜL PARKI',                              'Babil Sk. / Yekta Sk.',              NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'LEYLAK PARKI',                             'Ressam Salih Ermez Cd.',             NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ÇAMLIK PARKI',                             'Çömlekçi Çukuru Sk. / Katip Nazmiler Sk.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ONAY SİTESİ PARKI',                        'Çömlek Çukuru Sk.',                  NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'DUMLUPINAR PARKI',                         'Dr. Erkin Cd. / Mandıra Cd.',        NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ESKİ (PATYON) KURBAN PAZAR ALANI',         'Muhtar Kadir Kuruçay Sok.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'GÖZTEPE HALİL TÜRKAN İLKOKULU',            'Yumurtacı Abdi Bey Cad.',            NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MUSTAFA SAFFERT ANADOLU LİSESİ',           'Muhtar Kadir Kuruçay Sok.',          NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ESKİ SALI PAZARI',                         'D 100',                              NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ŞAHKULU DERGAHI BAHÇESİ',                  'Şair Arşi Cad.',                     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MERDİVENKÖY PARKI',                        'Bankacılar Sk.',                     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MERDİVENKÖY KORU PARKI',                   'Bankacılar Sk.',                     NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Merdivenköy not found, skipping 14 records';
  END IF;

  -- ================================================================
  -- OSMANAĞA (3 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Osmanağa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'KUŞDİLİ İSPARK OTOPARKI',                  'Mahmutbaba Sok. / Pazar Yolu Sok.',  NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'BAHARİYE ALTIYOL BOĞA KARŞISI YEŞİL ALAN', 'Kuşdili Cad.',                       NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'GAZİ MUSTAFA KEMAL PAŞA ORTAOKULU',         'Misak-ı Milli Sok.',                 NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Osmanağa not found, skipping 3 records';
  END IF;

  -- ================================================================
  -- RASİMPAŞA (3 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Rasimpaşa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'SÖĞÜTLÜÇEŞME KAPALI OTOPARKI',              'Taşköprü Cad. / Söğütlüçeşme Cad.', NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ESKİ İSTASYON BİNASI ÖNÜ OTOPARK',         'Halitağa Cad.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'İHSAN SUNGU İLKOKULU BAHÇESİ',              'Kızılay Sok.',                       NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Rasimpaşa not found, skipping 3 records';
  END IF;

  -- ================================================================
  -- SAHRAYICEDİT (10 kayıt) — PDF'de "SAHRAYICEDİD" ve "SAHRAYICEDİT" olarak geçiyor
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Sahrayıcedit' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, '23 NİSAN PARKI',                           'Cebesoy Sok. / Adile Naşit Sok.',    NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'DEFNE PARKI',                              'Bayar Cad. / Bahçeli Sok.',           NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'DOĞA PARK',                                'Cebesoy Sok. / Kireçhane Sok.',      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'DOSTLUK PARKI',                            'Mescitli Sok.',                      NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ERGUVAN PARKI',                            'Mümin Deresi Sok. / Çelik Sok.',     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ESİN IŞIK PARKI',                          'Mengi Sok.',                         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MENGÜ SOKAK PARKI',                        'Mengi Sok. / Uçar Sok.',             NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MİLLİ HAKİMİYET PARKI',                    'İnönü Cad. / Çamlık Parkı Sok.',     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HİLTON OTELİ YANI',                        'Batman Sk.',                         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'ARAPGİRLİ CAMİ PARKI',                     'Halk Sk.',                           NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Sahrayıcedit not found, skipping 10 records';
  END IF;

  -- ================================================================
  -- SUADİYE (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Suadiye' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, 'SUADİYE İSTASYON PARKI',                   'Ayşe Çavuş Sk.',                     NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'MEHMET KARAMANCI İLKOKULU',                 'Çamlı Sok. / Kurudere Sok.',         NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'HACI MUSTAFA TARMAN ANADOLU LİSESİ',        'Emin Ali Paşa Cad.',                 NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SUADİYE CAMİİ',                            'Suadiye Camii Sok.',                 NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Suadiye not found, skipping 4 records';
  END IF;

  -- ================================================================
  -- ZÜHTÜPAŞA (3 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Zühtüpaşa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, created_at)
    VALUES
      (nh, d_id, '23 NİSAN PARKI (1.BÖLGE)',                  'Hasan Kamil Sporel Sk.',             NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'İSTANBUL ANADOLU LİSESİ',                   'Recep Peker Cad.',                   NULL, NULL, NULL, src, sref, TRUE, now()),
      (nh, d_id, 'SÖĞÜTLÜÇEŞME MARMARAY İSTASYONU FENERBAHÇE STADI TERAFI YEŞİL ALAN', 'Taşköprü Cad.', NULL, NULL, NULL, src, sref, TRUE, now());
  ELSE
    RAISE NOTICE 'Neighborhood Zühtüpaşa not found, skipping 3 records';
  END IF;

END $$;
