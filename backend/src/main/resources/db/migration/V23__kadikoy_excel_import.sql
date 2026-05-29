-- =====================================================================
-- V23: Kadıköy toplanma alanları — Excel import (Google Maps links)
--
-- Kaynak: kadikoy_afet_toplanma_alanlari_google_maps.xlsx
-- 154 kayıt, 22 mahalle
-- source_name = 'Kadıköy Excel Import'
-- needs_review = false (tüm kayıtlar doğrulanmış)
-- latitude/longitude = NULL (Google Maps linki mevcuttur)
-- Duplicate prevention: (name, neighborhood_id, source_name) üçlüsüne göre
-- =====================================================================

DO $$
DECLARE
  d_id  UUID := '11111111-1111-1111-1111-000000000004'; -- Kadıköy
  src   TEXT := 'Kadıköy Excel Import';
  sref  TEXT := 'kadikoy_afet_toplanma_alanlari_google_maps.xlsx';
  nh    UUID;
BEGIN

  -- ================================================================
  -- 19 MAYIS (13 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = '19 Mayıs' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      '19 MAYIS PARKI',
      'Bayar Cad. Sultan Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=19%20MAYIS%20PARKI%2C%2019%20MAYIS%2C%20Bayar%20Cad.%20Sultan%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = '19 MAYIS PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AKASYA PARKI',
      'İnönü Cad. Panaroma Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AKASYA%20PARKI%2C%2019%20MAYIS%2C%20%C4%B0n%C3%B6n%C3%BC%20Cad.%20Panaroma%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AKASYA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'EKİN PARK',
      'İnönü Cad. Mehpare Sk / Aydın Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=EK%C4%B0N%20PARK%2C%2019%20MAYIS%2C%20%C4%B0n%C3%B6n%C3%BC%20Cad.%20Mehpare%20Sk%20%2F%20Ayd%C4%B1n%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'EKİN PARK'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HÜRRİYET PARKI',
      'Bayar Cad. Sultan Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=H%C3%9CRR%C4%B0YET%20PARKI%2C%2019%20MAYIS%2C%20Bayar%20Cad.%20Sultan%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HÜRRİYET PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HELİN PALANDÖKEN PARKI',
      'Bayar Cad. Toktaş Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=HEL%C4%B0N%20PALAND%C3%96KEN%20PARKI%2C%2019%20MAYIS%2C%20Bayar%20Cad.%20Tokta%C5%9F%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HELİN PALANDÖKEN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KRİTON CURİ PARKI',
      'Okur Sok. Rıfkı Bey Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KR%C4%B0TON%20CUR%C4%B0%20PARKI%2C%2019%20MAYIS%2C%20Okur%20Sok.%20R%C4%B1fk%C4%B1%20Bey%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KRİTON CURİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KUŞLUK PARKI',
      'Sultan Sok. Millet Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KU%C5%9ELUK%20PARKI%2C%2019%20MAYIS%2C%20Sultan%20Sok.%20Millet%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KUŞLUK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ZÜBEYDE HANIM PARKI',
      'Hilmipaşa Sok. Hüseyin Ayanoğlu Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=Z%C3%9CBEYDE%20HANIM%20PARKI%2C%2019%20MAYIS%2C%20Hilmipa%C5%9Fa%20Sok.%20H%C3%BCseyin%20Ayano%C4%9Flu%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ZÜBEYDE HANIM PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ORMEN SİTESİ PARKI',
      'Bayar Cd.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ORMEN%20S%C4%B0TES%C4%B0%20PARKI%2C%2019%20MAYIS%2C%20Bayar%20Cd.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ORMEN SİTESİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'IŞIK OKULLARI',
      'Sinan Ercan Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=I%C5%9EIK%20OKULLARI%2C%2019%20MAYIS%2C%20Sinan%20Ercan%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'IŞIK OKULLARI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MUSTAFA NAZMİ ERSİN CAMİİ',
      'Şahin Sok. Saray Sok',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MUSTAFA%20NAZM%C4%B0%20ERS%C4%B0N%20CAM%C4%B0%C4%B0%2C%2019%20MAYIS%2C%20%C5%9Eahin%20Sok.%20Saray%20Sok%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MUSTAFA NAZMİ ERSİN CAMİİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KAZASKER SHELL KARŞISI YEŞİL ALAN',
      'Güneşli Sok. Kaptan Arif Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KAZASKER%20SHELL%20KAR%C5%9EISI%20YE%C5%9E%C4%B0L%20ALAN%2C%2019%20MAYIS%2C%20G%C3%BCne%C5%9Fli%20Sok.%20Kaptan%20Arif%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KAZASKER SHELL KARŞISI YEŞİL ALAN'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞÖHRET KURŞUNOĞLU ÖZEL MESLEK OKULU',
      'Tüccarbaşı Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9E%C3%96HRET%20KUR%C5%9EUNO%C4%9ELU%20%C3%96ZEL%20MESLEK%20OKULU%2C%2019%20MAYIS%2C%20T%C3%BCccarba%C5%9F%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞÖHRET KURŞUNOĞLU ÖZEL MESLEK OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE '19 Mayıs mahallesi bulunamadı, 13 kayıt atlandı';
  END IF;

  -- ================================================================
  -- ACIBADEM (14 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Acıbadem' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'CEREN DAMAR PARKI',
      'Mustafa bey Sok Defne Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=CEREN%20DAMAR%20PARKI%2C%20ACIBADEM%2C%20Mustafa%20bey%20Sok%20Defne%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'CEREN DAMAR PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'J.ER CEMAL TÜFEKCİOĞLU PARKI',
      'Onur Sok. Çiftlik Sok',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=J.ER%20CEMAL%20T%C3%9CFEKC%C4%B0O%C4%9ELU%20PARKI%2C%20ACIBADEM%2C%20Onur%20Sok.%20%C3%87iftlik%20Sok%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'J.ER CEMAL TÜFEKCİOĞLU PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KURUÇEŞME PARKI',
      'Acıbadem Cad. Köftüncü Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KURU%C3%87E%C5%9EME%20PARKI%2C%20ACIBADEM%2C%20Ac%C4%B1badem%20Cad.%20K%C3%B6ft%C3%BCnc%C3%BC%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KURUÇEŞME PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SOKULLU PARKI',
      'Fatih Sok. Sokullu Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SOKULLU%20PARKI%2C%20ACIBADEM%2C%20Fatih%20Sok.%20Sokullu%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SOKULLU PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÜÇGEN PARKI',
      'Atıfbey Sok. Betül Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%9C%C3%87GEN%20PARKI%2C%20ACIBADEM%2C%20At%C4%B1fbey%20Sok.%20Bet%C3%BCl%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÜÇGEN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MEHTAP BÜLBÜL PARKI',
      'Acıbadem Cad. Defne Dok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEHTAP%20B%C3%9CLB%C3%9CL%20PARKI%2C%20ACIBADEM%2C%20Ac%C4%B1badem%20Cad.%20Defne%20Dok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MEHTAP BÜLBÜL PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÖZDEMİROĞLU İMAM HATİP ORTA OKULU',
      'Acıbadem Cad. Taşköprü Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%96ZDEM%C4%B0RO%C4%9ELU%20%C4%B0MAM%20HAT%C4%B0P%20ORTA%20OKULU%2C%20ACIBADEM%2C%20Ac%C4%B1badem%20Cad.%20Ta%C5%9Fk%C3%B6pr%C3%BC%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÖZDEMİROĞLU İMAM HATİP ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MARMARA GÜZEL SANATLAR FAKÜLTESİ BAHÇESİ',
      'Acıbadem Cad. Betül Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MARMARA%20G%C3%9CZEL%20SANATLAR%20FAK%C3%9CLTES%C4%B0%20BAH%C3%87ES%C4%B0%2C%20ACIBADEM%2C%20Ac%C4%B1badem%20Cad.%20Bet%C3%BCl%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MARMARA GÜZEL SANATLAR FAKÜLTESİ BAHÇESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AHMET SÜT CAMİİ',
      'Faik Ali Sok. Gömeç Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AHMET%20S%C3%9CT%20CAM%C4%B0%C4%B0%2C%20ACIBADEM%2C%20Faik%20Ali%20Sok.%20G%C3%B6me%C3%A7%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AHMET SÜT CAMİİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HUKUKÇULAR SİTESİ ORTA BAHÇE',
      'Necipbey Sok. Nakkas Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=HUKUK%C3%87ULAR%20S%C4%B0TES%C4%B0%20ORTA%20BAH%C3%87E%2C%20ACIBADEM%2C%20Necipbey%20Sok.%20Nakkas%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HUKUKÇULAR SİTESİ ORTA BAHÇE'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AHMET SANİ GEZİCİ KIZ İMAM HATİP LİSESİ',
      'Acıbadem Cad. Necipbey Sok',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AHMET%20SAN%C4%B0%20GEZ%C4%B0C%C4%B0%20KIZ%20%C4%B0MAM%20HAT%C4%B0P%20L%C4%B0SES%C4%B0%2C%20ACIBADEM%2C%20Ac%C4%B1badem%20Cad.%20Necipbey%20Sok%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AHMET SANİ GEZİCİ KIZ İMAM HATİP LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'NATİLLİUS AVM BAHÇESİ',
      'Meydan Cad. Fatih Sok',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=NAT%C4%B0LL%C4%B0US%20AVM%20BAH%C3%87ES%C4%B0%2C%20ACIBADEM%2C%20Meydan%20Cad.%20Fatih%20Sok%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'NATİLLİUS AVM BAHÇESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'TÜRK KADINLAR BİRLİĞİ PARKI',
      'Taşköprü Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=T%C3%9CRK%20KADINLAR%20B%C4%B0RL%C4%B0%C4%9E%C4%B0%20PARKI%2C%20ACIBADEM%2C%20Ta%C5%9Fk%C3%B6pr%C3%BC%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'TÜRK KADINLAR BİRLİĞİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'NATİLLİUS AVM KARŞISI İSPARK',
      'Dinlenç Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=NAT%C4%B0LL%C4%B0US%20AVM%20KAR%C5%9EISI%20%C4%B0SPARK%2C%20ACIBADEM%2C%20Dinlen%C3%A7%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'NATİLLİUS AVM KARŞISI İSPARK'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Acıbadem mahallesi bulunamadı, 14 kayıt atlandı';
  END IF;

  -- ================================================================
  -- BOSTANCI (15 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Bostancı' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MENEKŞE PARKI',
      'Emanet Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MENEK%C5%9EE%20PARKI%2C%20BOSTANCI%2C%20Emanet%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MENEKŞE PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MUALLA SELCANOĞLU MESLEKİ VE TEKNİK ANADOLU LİSESİ',
      'Prf. Dr. Kemal Akgüder Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MUALLA%20SELCANO%C4%9ELU%20MESLEK%C4%B0%20VE%20TEKN%C4%B0K%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20BOSTANCI%2C%20Prf.%20Dr.%20Kemal%20Akg%C3%BCder%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MUALLA SELCANOĞLU MESLEKİ VE TEKNİK ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HAYRULLAH KEFOĞLU ANADOLU LİSESİ',
      'Tayyareci Resmi Sok. Bostan Tariki Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=HAYRULLAH%20KEFO%C4%9ELU%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20BOSTANCI%2C%20Tayyareci%20Resmi%20Sok.%20Bostan%20Tariki%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HAYRULLAH KEFOĞLU ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'CLUP SPORİUM',
      'Bostancı Yanyol Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=CLUP%20SPOR%C4%B0UM%2C%20BOSTANCI%2C%20Bostanc%C4%B1%20Yanyol%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'CLUP SPORİUM'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KULOĞLU CAMİİ',
      'Vukela Cad. Bostancı Camii Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KULO%C4%9ELU%20CAM%C4%B0%C4%B0%2C%20BOSTANCI%2C%20Vukela%20Cad.%20Bostanc%C4%B1%20Camii%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KULOĞLU CAMİİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ATATÜRK ORTAOKULU',
      'Ali Nihat Tarlan Cad. Bostan Tariki Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ATAT%C3%9CRK%20ORTAOKULU%2C%20BOSTANCI%2C%20Ali%20Nihat%20Tarlan%20Cad.%20Bostan%20Tariki%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ATATÜRK ORTAOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'BOSTANCI GÖSTERİ MERKEZİ',
      'Bostanlararası Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=BOSTANCI%20G%C3%96STER%C4%B0%20MERKEZ%C4%B0%2C%20BOSTANCI%2C%20Bostanlararas%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'BOSTANCI GÖSTERİ MERKEZİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ESKİ BOSTANCI PAZAR YERİ',
      'Bostanlararası Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ESK%C4%B0%20BOSTANCI%20PAZAR%20YER%C4%B0%2C%20BOSTANCI%2C%20Bostanlararas%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ESKİ BOSTANCI PAZAR YERİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'LEMAN KAYA İLKOKULU',
      'Bostanlararası Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=LEMAN%20KAYA%20%C4%B0LKOKULU%2C%20BOSTANCI%2C%20Bostanlararas%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'LEMAN KAYA İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MELİH İSKANDİYAR İLKOKULU',
      'Kocayok Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEL%C4%B0H%20%C4%B0SKAND%C4%B0YAR%20%C4%B0LKOKULU%2C%20BOSTANCI%2C%20Kocayok%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MELİH İSKANDİYAR İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'TATARAĞASI CAMİİ',
      'Ali Nihat Tarlan Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=TATARA%C4%9EASI%20CAM%C4%B0%C4%B0%2C%20BOSTANCI%2C%20Ali%20Nihat%20Tarlan%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'TATARAĞASI CAMİİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      '23 NİSAN ZEHRA HANIM İMAM HATİP ORTA OKULU',
      'Gümüşçü Sok',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=23%20N%C4%B0SAN%20ZEHRA%20HANIM%20%C4%B0MAM%20HAT%C4%B0P%20ORTA%20OKULU%2C%20BOSTANCI%2C%20G%C3%BCm%C3%BC%C5%9F%C3%A7%C3%BC%20Sok%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = '23 NİSAN ZEHRA HANIM İMAM HATİP ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KILIÇ SPOR TESİSLERİ',
      'Bostanlararası Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KILI%C3%87%20SPOR%20TES%C4%B0SLER%C4%B0%2C%20BOSTANCI%2C%20Bostanlararas%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KILIÇ SPOR TESİSLERİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MAT OTOMATİV',
      'Bostanlararası Sok. Emin Ali Paşa Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MAT%20OTOMAT%C4%B0V%2C%20BOSTANCI%2C%20Bostanlararas%C4%B1%20Sok.%20Emin%20Ali%20Pa%C5%9Fa%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MAT OTOMATİV'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MİLTAŞ SPOR TESİSLERİ',
      'Havacı Binbaşı Mehmet Sok. Yeni Gelin Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=M%C4%B0LTA%C5%9E%20SPOR%20TES%C4%B0SLER%C4%B0%2C%20BOSTANCI%2C%20Havac%C4%B1%20Binba%C5%9F%C4%B1%20Mehmet%20Sok.%20Yeni%20Gelin%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MİLTAŞ SPOR TESİSLERİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Bostancı mahallesi bulunamadı, 15 kayıt atlandı';
  END IF;

  -- ================================================================
  -- CADDEPOSTAN (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Caddebostan' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'GÖZTEPE 60. YIL PARKI',
      'Hulusi Behçet Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=G%C3%96ZTEPE%2060.%20YIL%20PARKI%2C%20CADDEPOSTAN%2C%20Hulusi%20Beh%C3%A7et%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'GÖZTEPE 60. YIL PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞEHİT ORHUN GÖKTAY İLKOKULU',
      'Bilim Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9EEH%C4%B0T%20ORHUN%20G%C3%96KTAY%20%C4%B0LKOKULU%2C%20CADDEPOSTAN%2C%20Bilim%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞEHİT ORHUN GÖKTAY İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      '50. YIL TARHAN ANADOLU LİSESİ',
      'Operatör Cemil Topuzlu Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=50.%20YIL%20TARHAN%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20CADDEPOSTAN%2C%20Operat%C3%B6r%20Cemil%20Topuzlu%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = '50. YIL TARHAN ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'TARIM İL MÜDÜRLÜĞÜ',
      'Bağdat Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=TARIM%20%C4%B0L%20M%C3%9CD%C3%9CRL%C3%9C%C4%9E%C3%9C%2C%20CADDEPOSTAN%2C%20Ba%C4%9Fdat%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'TARIM İL MÜDÜRLÜĞÜ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Caddebostan mahallesi bulunamadı, 4 kayıt atlandı';
  END IF;

  -- ================================================================
  -- CAFERAĞA (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Caferağa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MODA PARKI',
      'Ferit Tek Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MODA%20PARKI%2C%20CAFERA%C4%9EA%2C%20Ferit%20Tek%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MODA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞAİR NEFİ PARKI',
      'Şair Nefi Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9EA%C4%B0R%20NEF%C4%B0%20PARKI%2C%20CAFERA%C4%9EA%2C%20%C5%9Eair%20Nefi%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞAİR NEFİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KADIKÖY ANADOLU LİSESİ',
      'Dr. Esat Işık Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KADIK%C3%96Y%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20CAFERA%C4%9EA%2C%20Dr.%20Esat%20I%C5%9F%C4%B1k%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KADIKÖY ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'FRANSIZ LİSESİ',
      'Doktor Esat Işık Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=FRANSIZ%20L%C4%B0SES%C4%B0%2C%20CAFERA%C4%9EA%2C%20Doktor%20Esat%20I%C5%9F%C4%B1k%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'FRANSIZ LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Caferağa mahallesi bulunamadı, 4 kayıt atlandı';
  END IF;

  -- ================================================================
  -- DUMLUPINAR (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Dumlupınar' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'FENERBAJÇE SPOR TESİSLERİ',
      'Yumurtacı Abdi Bey Cad. Bahçem Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=FENERBAJ%C3%87E%20SPOR%20TES%C4%B0SLER%C4%B0%2C%20DUMLUPINAR%2C%20Yumurtac%C4%B1%20Abdi%20Bey%20Cad.%20Bah%C3%A7em%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'FENERBAJÇE SPOR TESİSLERİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AHMET SANİ GEZİCİ ÇOK PROGRAMLI ANADOLU LİSESİ',
      'Yumurtacı Abdi Bey Cad. Pelin Sok. Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AHMET%20SAN%C4%B0%20GEZ%C4%B0C%C4%B0%20%C3%87OK%20PROGRAMLI%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20DUMLUPINAR%2C%20Yumurtac%C4%B1%20Abdi%20Bey%20Cad.%20Pelin%20Sok.%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AHMET SANİ GEZİCİ ÇOK PROGRAMLI ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MEDENİYET ÜNİVERSİTESİ',
      'D-100 Karayolu Yumurtarcı Abdibey Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEDEN%C4%B0YET%20%C3%9CN%C4%B0VERS%C4%B0TES%C4%B0%2C%20DUMLUPINAR%2C%20D-100%20Karayolu%20Yumurtarc%C4%B1%20Abdibey%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MEDENİYET ÜNİVERSİTESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MEHMET BEYAZIT ANADOLU LİSESİ BAHÇESİ',
      'Hızırbey Cad. Merdivenköy Yolu Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEHMET%20BEYAZIT%20ANADOLU%20L%C4%B0SES%C4%B0%20BAH%C3%87ES%C4%B0%2C%20DUMLUPINAR%2C%20H%C4%B1z%C4%B1rbey%20Cad.%20Merdivenk%C3%B6y%20Yolu%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MEHMET BEYAZIT ANADOLU LİSESİ BAHÇESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Dumlupınar mahallesi bulunamadı, 4 kayıt atlandı';
  END IF;

  -- ================================================================
  -- ERENKÖY (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Erenköy' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'GARDENYA ÇIKMAZI PARKI',
      'Gardenya Çıkmazı Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=GARDENYA%20%C3%87IKMAZI%20PARKI%2C%20ERENK%C3%96Y%2C%20Gardenya%20%C3%87%C4%B1kmaz%C4%B1%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'GARDENYA ÇIKMAZI PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ERENKÖY KIZ LİSESİ',
      'Ömerpaşa Sok. Rıdavanpaşa Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ERENK%C3%96Y%20KIZ%20L%C4%B0SES%C4%B0%2C%20ERENK%C3%96Y%2C%20%C3%96merpa%C5%9Fa%20Sok.%20R%C4%B1davanpa%C5%9Fa%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ERENKÖY KIZ LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ERENKÖY GÖNÜLLÜ MERKESİ ARKASI PARK',
      'Eralp Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ERENK%C3%96Y%20G%C3%96N%C3%9CLL%C3%9C%20MERKES%C4%B0%20ARKASI%20PARK%2C%20ERENK%C3%96Y%2C%20Eralp%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ERENKÖY GÖNÜLLÜ MERKESİ ARKASI PARK'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ZİHNİ PAŞA ÇAMİ AVLUSU',
      'Telli Kavak Sok. Erenköy İstasyon Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=Z%C4%B0HN%C4%B0%20PA%C5%9EA%20%C3%87AM%C4%B0%20AVLUSU%2C%20ERENK%C3%96Y%2C%20Telli%20Kavak%20Sok.%20Erenk%C3%B6y%20%C4%B0stasyon%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ZİHNİ PAŞA ÇAMİ AVLUSU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Erenköy mahallesi bulunamadı, 4 kayıt atlandı';
  END IF;

  -- ================================================================
  -- EĞİTİM (6 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Eğitim' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KEMAL SUNAL PARKI',
      'Hilmibey Sk. Adilbey Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KEMAL%20SUNAL%20PARKI%2C%20E%C4%9E%C4%B0T%C4%B0M%2C%20Hilmibey%20Sk.%20Adilbey%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KEMAL SUNAL PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SSK ARKASI PARKI',
      'Hızırbey Cd. Mektep Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SSK%20ARKASI%20PARKI%2C%20E%C4%9E%C4%B0T%C4%B0M%2C%20H%C4%B1z%C4%B1rbey%20Cd.%20Mektep%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SSK ARKASI PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞHT. HAKAN BAYLAN PARKI',
      'Yeşilköşk Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9EHT.%20HAKAN%20BAYLAN%20PARKI%2C%20E%C4%9E%C4%B0T%C4%B0M%2C%20Ye%C5%9Filk%C3%B6%C5%9Fk%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞHT. HAKAN BAYLAN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DAYANIŞMA PARKI',
      'Babacan Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DAYANI%C5%9EMA%20PARKI%2C%20E%C4%9E%C4%B0T%C4%B0M%2C%20Babacan%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DAYANIŞMA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ATATÜRK FEN LİSESİ',
      'Sarayönü Cad. Muratpaşa Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ATAT%C3%9CRK%20FEN%20L%C4%B0SES%C4%B0%2C%20E%C4%9E%C4%B0T%C4%B0M%2C%20Saray%C3%B6n%C3%BC%20Cad.%20Muratpa%C5%9Fa%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ATATÜRK FEN LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MARMARA ÜNİVERSİTESİ',
      'Fahrettin Kerim Gökay Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MARMARA%20%C3%9CN%C4%B0VERS%C4%B0TES%C4%B0%2C%20E%C4%9E%C4%B0T%C4%B0M%2C%20Fahrettin%20Kerim%20G%C3%B6kay%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MARMARA ÜNİVERSİTESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Eğitim mahallesi bulunamadı, 6 kayıt atlandı';
  END IF;

  -- ================================================================
  -- FENERBAHÇE (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Fenerbahçe' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÇAMLIK (IHLAMUR) PARKI',
      'Bozkır Sk. Yeşilkır Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%87AMLIK%20%28IHLAMUR%29%20PARKI%2C%20FENERBAH%C3%87E%2C%20Bozk%C4%B1r%20Sk.%20Ye%C5%9Filk%C4%B1r%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÇAMLIK (IHLAMUR) PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'BEHİCE YAZGAN PARKI',
      'Fener Kalamış Cd.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=BEH%C4%B0CE%20YAZGAN%20PARKI%2C%20FENERBAH%C3%87E%2C%20Fener%20Kalam%C4%B1%C5%9F%20Cd.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'BEHİCE YAZGAN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'FENERYOLU HALK EĞİTİM MERKEZİ',
      'Dr. Faruk Ayanoğlu Cad. Ali Fuat Başgil Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=FENERYOLU%20HALK%20E%C4%9E%C4%B0T%C4%B0M%20MERKEZ%C4%B0%2C%20FENERBAH%C3%87E%2C%20Dr.%20Faruk%20Ayano%C4%9Flu%20Cad.%20Ali%20Fuat%20Ba%C5%9Fgil%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'FENERYOLU HALK EĞİTİM MERKEZİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'NURETTİN TEKSAN ORTA OKULU',
      'Gazi Mehmetcik Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=NURETT%C4%B0N%20TEKSAN%20ORTA%20OKULU%2C%20FENERBAH%C3%87E%2C%20Gazi%20Mehmetcik%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'NURETTİN TEKSAN ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Fenerbahçe mahallesi bulunamadı, 4 kayıt atlandı';
  END IF;

  -- ================================================================
  -- FENERYOLU (8 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Feneryolu' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KUYUBAŞI PARKI',
      'F.Kerim Gökay Cd. Kuyubaşı Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KUYUBA%C5%9EI%20PARKI%2C%20FENERYOLU%2C%20F.Kerim%20G%C3%B6kay%20Cd.%20Kuyuba%C5%9F%C4%B1%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KUYUBAŞI PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'FENERYOLU MUHTARLIK PARKI',
      'Dostane Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=FENERYOLU%20MUHTARLIK%20PARKI%2C%20FENERYOLU%2C%20Dostane%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'FENERYOLU MUHTARLIK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      '26 MART PARKI',
      'Feneryolu Sk. Erdoğdu Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=26%20MART%20PARKI%2C%20FENERYOLU%2C%20Feneryolu%20Sk.%20Erdo%C4%9Fdu%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = '26 MART PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SANAT PARKI',
      'F.Kerim Gökay Cd. Şehir Kahya Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SANAT%20PARKI%2C%20FENERYOLU%2C%20F.Kerim%20G%C3%B6kay%20Cd.%20%C5%9Eehir%20Kahya%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SANAT PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MELAHAT ŞEFİZADE İLKOKULU',
      'Hamdibey Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MELAHAT%20%C5%9EEF%C4%B0ZADE%20%C4%B0LKOKULU%2C%20FENERYOLU%2C%20Hamdibey%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MELAHAT ŞEFİZADE İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MUSTAFA AYKIN İLKOKULU',
      'Gazi Mustafapaşa Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MUSTAFA%20AYKIN%20%C4%B0LKOKULU%2C%20FENERYOLU%2C%20Gazi%20Mustafapa%C5%9Fa%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MUSTAFA AYKIN İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ADEN TENİS KULÜBÜ VE BASKET SAHALARI',
      'Fahir Açan Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ADEN%20TEN%C4%B0S%20KUL%C3%9CB%C3%9C%20VE%20BASKET%20SAHALARI%2C%20FENERYOLU%2C%20Fahir%20A%C3%A7an%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ADEN TENİS KULÜBÜ VE BASKET SAHALARI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'TERAS CAFE ÇEVRESİ',
      'Mashar Osman Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=TERAS%20CAFE%20%C3%87EVRES%C4%B0%2C%20FENERYOLU%2C%20Mashar%20Osman%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'TERAS CAFE ÇEVRESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Feneryolu mahallesi bulunamadı, 8 kayıt atlandı';
  END IF;

  -- ================================================================
  -- FİKİRTEPE (1 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Fikirtepe' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'TEPE CAMİİ AVLUSU',
      'Niyazi Bey Sok. Özen Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=TEPE%20CAM%C4%B0%C4%B0%20AVLUSU%2C%20F%C4%B0K%C4%B0RTEPE%2C%20Niyazi%20Bey%20Sok.%20%C3%96zen%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'TEPE CAMİİ AVLUSU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Fikirtepe mahallesi bulunamadı, 1 kayıt atlandı';
  END IF;

  -- ================================================================
  -- GÖZTEPE (11 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Göztepe' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DEMOKRASİ PARKI',
      'Kortanpaşa Sk. Ahmet Refik Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DEMOKRAS%C4%B0%20PARKI%2C%20G%C3%96ZTEPE%2C%20Kortanpa%C5%9Fa%20Sk.%20Ahmet%20Refik%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DEMOKRASİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÖZGECAN ASLAN PARKI',
      'Yeşilçeşme Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%96ZGECAN%20ASLAN%20PARKI%2C%20G%C3%96ZTEPE%2C%20Ye%C5%9Fil%C3%A7e%C5%9Fme%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÖZGECAN ASLAN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'NADİRAĞA PARKI',
      'Nadirağa Sk. Ege Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=NAD%C4%B0RA%C4%9EA%20PARKI%2C%20G%C3%96ZTEPE%2C%20Nadira%C4%9Fa%20Sk.%20Ege%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'NADİRAĞA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KARANFİL SOKAK PARKI',
      'Karanfil Sk. Damla Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KARANF%C4%B0L%20SOKAK%20PARKI%2C%20G%C3%96ZTEPE%2C%20Karanfil%20Sk.%20Damla%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KARANFİL SOKAK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÖZGÜRLÜK PARKI',
      'Mustafa Mashar Bey Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%96ZG%C3%9CRL%C3%9CK%20PARKI%2C%20G%C3%96ZTEPE%2C%20Mustafa%20Mashar%20Bey%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÖZGÜRLÜK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'İHSAN KURŞUNOĞLU ANADOLU LİSESİ',
      'Tanzimat Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C4%B0HSAN%20KUR%C5%9EUNO%C4%9ELU%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20G%C3%96ZTEPE%2C%20Tanzimat%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'İHSAN KURŞUNOĞLU ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ALİ HAYDAR ERSOY ORTA OKULU',
      'Bağdat Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AL%C4%B0%20HAYDAR%20ERSOY%20ORTA%20OKULU%2C%20G%C3%96ZTEPE%2C%20Ba%C4%9Fdat%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ALİ HAYDAR ERSOY ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MEHMET AYDOSLU İŞİTME ENGELLİLER ORTA OKULU',
      'Tanzimat Sok. Bestekar Ziya Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEHMET%20AYDOSLU%20%C4%B0%C5%9E%C4%B0TME%20ENGELL%C4%B0LER%20ORTA%20OKULU%2C%20G%C3%96ZTEPE%2C%20Tanzimat%20Sok.%20Bestekar%20Ziya%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MEHMET AYDOSLU İŞİTME ENGELLİLER ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AVNİ AKYOL GÜZEL SANATLAR LİSESİ',
      'Rıdvanpaşa So. Ömer Paşa Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AVN%C4%B0%20AKYOL%20G%C3%9CZEL%20SANATLAR%20L%C4%B0SES%C4%B0%2C%20G%C3%96ZTEPE%2C%20R%C4%B1dvanpa%C5%9Fa%20So.%20%C3%96mer%20Pa%C5%9Fa%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AVNİ AKYOL GÜZEL SANATLAR LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'İLHAMİ AHMED ÖRNEKAL İLKOKULU',
      'Bağdat Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C4%B0LHAM%C4%B0%20AHMED%20%C3%96RNEKAL%20%C4%B0LKOKULU%2C%20G%C3%96ZTEPE%2C%20Ba%C4%9Fdat%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'İLHAMİ AHMED ÖRNEKAL İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'YEŞİLÇEŞME SOK. SPOR ALANI',
      'Yeşilçeşme Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=YE%C5%9E%C4%B0L%C3%87E%C5%9EME%20SOK.%20SPOR%20ALANI%2C%20G%C3%96ZTEPE%2C%20Ye%C5%9Fil%C3%A7e%C5%9Fme%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'YEŞİLÇEŞME SOK. SPOR ALANI'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Göztepe mahallesi bulunamadı, 11 kayıt atlandı';
  END IF;

  -- ================================================================
  -- HASANPAŞA (6 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Hasanpaşa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'YENİ SALI PAZARI',
      'Uzuncayır Cad. Mandıra Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=YEN%C4%B0%20SALI%20PAZARI%2C%20HASANPA%C5%9EA%2C%20Uzuncay%C4%B1r%20Cad.%20Mand%C4%B1ra%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'YENİ SALI PAZARI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KADIKÖY ANADOLU İMAM HATİP LİSESİ',
      'Uzuncayır Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KADIK%C3%96Y%20ANADOLU%20%C4%B0MAM%20HAT%C4%B0P%20L%C4%B0SES%C4%B0%2C%20HASANPA%C5%9EA%2C%20Uzuncay%C4%B1r%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KADIKÖY ANADOLU İMAM HATİP LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'GAZHANE',
      'Uzuncayır Cad. Gazhane Deresi Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=GAZHANE%2C%20HASANPA%C5%9EA%2C%20Uzuncay%C4%B1r%20Cad.%20Gazhane%20Deresi%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'GAZHANE'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SÖGÜTLÜÇEŞME TREN İSTASYONU',
      'Taşköprü Cad. Fahrettin Kerim Gökay Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=S%C3%96G%C3%9CTL%C3%9C%C3%87E%C5%9EME%20TREN%20%C4%B0STASYONU%2C%20HASANPA%C5%9EA%2C%20Ta%C5%9Fk%C3%B6pr%C3%BC%20Cad.%20Fahrettin%20Kerim%20G%C3%B6kay%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SÖGÜTLÜÇEŞME TREN İSTASYONU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DOĞA KOLEJİ VE CAMİİ AVLUSU',
      'Zeamet Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DO%C4%9EA%20KOLEJ%C4%B0%20VE%20CAM%C4%B0%C4%B0%20AVLUSU%2C%20HASANPA%C5%9EA%2C%20Zeamet%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DOĞA KOLEJİ VE CAMİİ AVLUSU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KADIKÖY İMAM HATİP ORTA OKULU',
      'Uzunçayır Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KADIK%C3%96Y%20%C4%B0MAM%20HAT%C4%B0P%20ORTA%20OKULU%2C%20HASANPA%C5%9EA%2C%20Uzun%C3%A7ay%C4%B1r%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KADIKÖY İMAM HATİP ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Hasanpaşa mahallesi bulunamadı, 6 kayıt atlandı';
  END IF;

  -- ================================================================
  -- KOZYATAĞI (12 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Kozyatağı' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AHMET TANER KIŞLALI PARKI',
      'Hüseyin Ayanoğlu Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AHMET%20TANER%20KI%C5%9ELALI%20PARKI%2C%20KOZYATA%C4%9EI%2C%20H%C3%BCseyin%20Ayano%C4%9Flu%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AHMET TANER KIŞLALI PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'BARIŞ PARKI-1',
      'Korku Sk. Belediye Blokları Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=BARI%C5%9E%20PARKI-1%2C%20KOZYATA%C4%9EI%2C%20Korku%20Sk.%20Belediye%20Bloklar%C4%B1%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'BARIŞ PARKI-1'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'BARIŞ PARKI-2',
      'Korkut Sk. Fatih Devravut Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=BARI%C5%9E%20PARKI-2%2C%20KOZYATA%C4%9EI%2C%20Korkut%20Sk.%20Fatih%20Devravut%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'BARIŞ PARKI-2'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'FİRUZAN TOPRAK PARKI',
      'Gazi Ethem Paşa Sk. Lemi Atlı Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=F%C4%B0RUZAN%20TOPRAK%20PARKI%2C%20KOZYATA%C4%9EI%2C%20Gazi%20Ethem%20Pa%C5%9Fa%20Sk.%20Lemi%20Atl%C4%B1%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'FİRUZAN TOPRAK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ILGIN PARKI',
      'Değirmen Çıkmazı Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ILGIN%20PARKI%2C%20KOZYATA%C4%9EI%2C%20De%C4%9Firmen%20%C3%87%C4%B1kmaz%C4%B1%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ILGIN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SARI KANARYA PARKI',
      'Sarı Kanarya Sk. Saniye Ermutlu Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SARI%20KANARYA%20PARKI%2C%20KOZYATA%C4%9EI%2C%20Sar%C4%B1%20Kanarya%20Sk.%20Saniye%20Ermutlu%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SARI KANARYA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'AFET EĞİTİM VE BİLİNÇLENDİRME PARKI',
      'Saniye Ermutlu Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=AFET%20E%C4%9E%C4%B0T%C4%B0M%20VE%20B%C4%B0L%C4%B0N%C3%87LEND%C4%B0RME%20PARKI%2C%20KOZYATA%C4%9EI%2C%20Saniye%20Ermutlu%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'AFET EĞİTİM VE BİLİNÇLENDİRME PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HAKKI DEĞER ORTA OKULU',
      'Gazi Ethem Paşa Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=HAKKI%20DE%C4%9EER%20ORTA%20OKULU%2C%20KOZYATA%C4%9EI%2C%20Gazi%20Ethem%20Pa%C5%9Fa%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HAKKI DEĞER ORTA OKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'İSMAİL ERDEM ORTAOKULU',
      'Değirmen Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C4%B0SMA%C4%B0L%20ERDEM%20ORTAOKULU%2C%20KOZYATA%C4%9EI%2C%20De%C4%9Firmen%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'İSMAİL ERDEM ORTAOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KOZYATAĞI METRO GİRİŞ ALANI',
      'E 80 Yolu',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KOZYATA%C4%9EI%20METRO%20G%C4%B0R%C4%B0%C5%9E%20ALANI%2C%20KOZYATA%C4%9EI%2C%20E%2080%20Yolu%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KOZYATAĞI METRO GİRİŞ ALANI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MEHMET ÇAVUŞ CAMİ',
      'Şakacı Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEHMET%20%C3%87AVU%C5%9E%20CAM%C4%B0%2C%20KOZYATA%C4%9EI%2C%20%C5%9Eakac%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MEHMET ÇAVUŞ CAMİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞÜKRAN KARABELLİ İLKOKULU',
      'Okul Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9E%C3%9CKRAN%20KARABELL%C4%B0%20%C4%B0LKOKULU%2C%20KOZYATA%C4%9EI%2C%20Okul%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞÜKRAN KARABELLİ İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Kozyatağı mahallesi bulunamadı, 12 kayıt atlandı';
  END IF;

  -- ================================================================
  -- KOŞUYOLU (11 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Koşuyolu' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KOŞUYOLU PARKI',
      'Mühittin Üstündağ Sok. Mehmet Akman Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KO%C5%9EUYOLU%20PARKI%2C%20KO%C5%9EUYOLU%2C%20M%C3%BChittin%20%C3%9Cst%C3%BCnda%C4%9F%20Sok.%20Mehmet%20Akman%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KOŞUYOLU PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MİMOZA PARKI',
      'Mütevelli Çeşme Cad. Cenap Şahabettin Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=M%C4%B0MOZA%20PARKI%2C%20KO%C5%9EUYOLU%2C%20M%C3%BCtevelli%20%C3%87e%C5%9Fme%20Cad.%20Cenap%20%C5%9Eahabettin%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MİMOZA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞEKER PARKI',
      'Şeker Parkı',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9EEKER%20PARKI%2C%20KO%C5%9EUYOLU%2C%20%C5%9Eeker%20Park%C4%B1%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞEKER PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'YAŞAM PARKI',
      'Koşuyolu Cad. Türkan Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=YA%C5%9EAM%20PARKI%2C%20KO%C5%9EUYOLU%2C%20Ko%C5%9Fuyolu%20Cad.%20T%C3%BCrkan%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'YAŞAM PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÖĞRETMENLER PARKI',
      'Görgülü Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%96%C4%9ERETMENLER%20PARKI%2C%20KO%C5%9EUYOLU%2C%20G%C3%B6rg%C3%BCl%C3%BC%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÖĞRETMENLER PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MANOLYA PARKI',
      'Şevket Kantarcı Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MANOLYA%20PARKI%2C%20KO%C5%9EUYOLU%2C%20%C5%9Eevket%20Kantarc%C4%B1%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MANOLYA PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ESKİ KOŞUYOLU KALP HASTANESİ',
      'Koşuyolu Cad. Salih Omurtak Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ESK%C4%B0%20KO%C5%9EUYOLU%20KALP%20HASTANES%C4%B0%2C%20KO%C5%9EUYOLU%2C%20Ko%C5%9Fuyolu%20Cad.%20Salih%20Omurtak%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ESKİ KOŞUYOLU KALP HASTANESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'CENAP ŞEHABETTIN İLKOKULU BAHÇESİ',
      'Cenap Şahabettin Sok. Ferhat Acarkent Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=CENAP%20%C5%9EEHABETTIN%20%C4%B0LKOKULU%20BAH%C3%87ES%C4%B0%2C%20KO%C5%9EUYOLU%2C%20Cenap%20%C5%9Eahabettin%20Sok.%20Ferhat%20Acarkent%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'CENAP ŞEHABETTIN İLKOKULU BAHÇESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HALİL TÜRKKAN ORTAOKULU',
      'Görgülü Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=HAL%C4%B0L%20T%C3%9CRKKAN%20ORTAOKULU%2C%20KO%C5%9EUYOLU%2C%20G%C3%B6rg%C3%BCl%C3%BC%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HALİL TÜRKKAN ORTAOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'REŞAT NURİ GÜLTEKİN ORTAOKULU',
      'Salih Omurtak Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=RE%C5%9EAT%20NUR%C4%B0%20G%C3%9CLTEK%C4%B0N%20ORTAOKULU%2C%20KO%C5%9EUYOLU%2C%20Salih%20Omurtak%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'REŞAT NURİ GÜLTEKİN ORTAOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SAMYELİ FUTBOL SAHASI',
      'Görgülü Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SAMYEL%C4%B0%20FUTBOL%20SAHASI%2C%20KO%C5%9EUYOLU%2C%20G%C3%B6rg%C3%BCl%C3%BC%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SAMYELİ FUTBOL SAHASI'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Koşuyolu mahallesi bulunamadı, 11 kayıt atlandı';
  END IF;

  -- ================================================================
  -- MERDİVENKÖY (14 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Merdivenköy' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÖZLEM SOKAK PARKI',
      'Şair Arşi Cd.Özlem Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%96ZLEM%20SOKAK%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20%C5%9Eair%20Ar%C5%9Fi%20Cd.%C3%96zlem%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÖZLEM SOKAK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÇINAR PARKI',
      'Dr. Erkin Cd.Karaman Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%87INAR%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Dr.%20Erkin%20Cd.Karaman%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÇINAR PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'EYLÜL PARKI',
      'Babil Sk. Yekta Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=EYL%C3%9CL%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Babil%20Sk.%20Yekta%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'EYLÜL PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'LEYLAK PARKI',
      'Ressam Salih Ermez Cd.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=LEYLAK%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Ressam%20Salih%20Ermez%20Cd.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'LEYLAK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ÇAMLIK PARKI',
      'Çömlekçi Çukuru Sk. Katip Nazmiler Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C3%87AMLIK%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20%C3%87%C3%B6mlek%C3%A7i%20%C3%87ukuru%20Sk.%20Katip%20Nazmiler%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ÇAMLIK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ONAY SİTESİ PARKI',
      'Çömlek Çukuru Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ONAY%20S%C4%B0TES%C4%B0%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20%C3%87%C3%B6mlek%20%C3%87ukuru%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ONAY SİTESİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DUMLUPINAR PARKI',
      'Dr. Erkin Cd. Mandıra Cd.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DUMLUPINAR%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Dr.%20Erkin%20Cd.%20Mand%C4%B1ra%20Cd.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DUMLUPINAR PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ESKİ (PATYON) KURBAN PAZAR ALANI',
      'Muhtar Kadir Kuruçay Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ESK%C4%B0%20%28PATYON%29%20KURBAN%20PAZAR%20ALANI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Muhtar%20Kadir%20Kuru%C3%A7ay%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ESKİ (PATYON) KURBAN PAZAR ALANI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'GÖZTEPE HALİL TÜRKAN İLKOKULU',
      'Yumurtacı Abdi Bey Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=G%C3%96ZTEPE%20HAL%C4%B0L%20T%C3%9CRKAN%20%C4%B0LKOKULU%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Yumurtac%C4%B1%20Abdi%20Bey%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'GÖZTEPE HALİL TÜRKAN İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MUSTAFA SAFFERT ANADOLU LİSESİ',
      'Muhtar Kadir Kuruçay Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MUSTAFA%20SAFFERT%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Muhtar%20Kadir%20Kuru%C3%A7ay%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MUSTAFA SAFFERT ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ESKİ SALI PAZARI',
      'D 100',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ESK%C4%B0%20SALI%20PAZARI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20D%20100%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ESKİ SALI PAZARI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ŞAHKULU DERGAHI BAHÇESİ',
      'Şair Arşi Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C5%9EAHKULU%20DERGAHI%20BAH%C3%87ES%C4%B0%2C%20MERD%C4%B0VENK%C3%96Y%2C%20%C5%9Eair%20Ar%C5%9Fi%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ŞAHKULU DERGAHI BAHÇESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MERDİVENKÖY PARKI',
      'Bankacılar Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MERD%C4%B0VENK%C3%96Y%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Bankac%C4%B1lar%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MERDİVENKÖY PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MERDİVENKÖY KORU PARKI',
      'Bankacılar Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MERD%C4%B0VENK%C3%96Y%20KORU%20PARKI%2C%20MERD%C4%B0VENK%C3%96Y%2C%20Bankac%C4%B1lar%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MERDİVENKÖY KORU PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Merdivenköy mahallesi bulunamadı, 14 kayıt atlandı';
  END IF;

  -- ================================================================
  -- OSMANAĞA (3 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Osmanağa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'KUŞDİLİ İSPARK OTOPARKI',
      'Mahmutbaba Sok. Pazar Yolu Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=KU%C5%9ED%C4%B0L%C4%B0%20%C4%B0SPARK%20OTOPARKI%2C%20OSMANA%C4%9EA%2C%20Mahmutbaba%20Sok.%20Pazar%20Yolu%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'KUŞDİLİ İSPARK OTOPARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'BAHARİYE ALTIYOL BOĞA KARŞISI YEŞİL ALAN',
      'Kuşdili Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=BAHAR%C4%B0YE%20ALTIYOL%20BO%C4%9EA%20KAR%C5%9EISI%20YE%C5%9E%C4%B0L%20ALAN%2C%20OSMANA%C4%9EA%2C%20Ku%C5%9Fdili%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'BAHARİYE ALTIYOL BOĞA KARŞISI YEŞİL ALAN'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'GAZİ MUSTAFA KEMAL PAŞA ORTAOKULU',
      'Misak-ı Milli Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=GAZ%C4%B0%20MUSTAFA%20KEMAL%20PA%C5%9EA%20ORTAOKULU%2C%20OSMANA%C4%9EA%2C%20Misak-%C4%B1%20Milli%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'GAZİ MUSTAFA KEMAL PAŞA ORTAOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Osmanağa mahallesi bulunamadı, 3 kayıt atlandı';
  END IF;

  -- ================================================================
  -- RASİMPAŞA (3 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Rasimpaşa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SÖĞÜTLÜÇEŞME KAPALI OTOPARKI',
      'Taşköprü Cad. Söğütlüçeşme Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=S%C3%96%C4%9E%C3%9CTL%C3%9C%C3%87E%C5%9EME%20KAPALI%20OTOPARKI%2C%20RAS%C4%B0MPA%C5%9EA%2C%20Ta%C5%9Fk%C3%B6pr%C3%BC%20Cad.%20S%C3%B6%C4%9F%C3%BCtl%C3%BC%C3%A7e%C5%9Fme%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SÖĞÜTLÜÇEŞME KAPALI OTOPARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ESKİ İSTASYON BİNASI ÖNÜ OTOPARK',
      'Halitağa Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ESK%C4%B0%20%C4%B0STASYON%20B%C4%B0NASI%20%C3%96N%C3%9C%20OTOPARK%2C%20RAS%C4%B0MPA%C5%9EA%2C%20Halita%C4%9Fa%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ESKİ İSTASYON BİNASI ÖNÜ OTOPARK'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'İHSAN SUNGU İLKOKULU BAHÇESİ',
      'Kızılay Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C4%B0HSAN%20SUNGU%20%C4%B0LKOKULU%20BAH%C3%87ES%C4%B0%2C%20RAS%C4%B0MPA%C5%9EA%2C%20K%C4%B1z%C4%B1lay%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'İHSAN SUNGU İLKOKULU BAHÇESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Rasimpaşa mahallesi bulunamadı, 3 kayıt atlandı';
  END IF;

  -- ================================================================
  -- SAHRAYICEDİD (8 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Sahrayıcedit' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      '23 NİSAN PARKI',
      'Cebesoy Sok. Adile Naşit Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=23%20N%C4%B0SAN%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20Cebesoy%20Sok.%20Adile%20Na%C5%9Fit%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = '23 NİSAN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DEFNE PARKI',
      'Bayar Cad. Bahçeli Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DEFNE%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20Bayar%20Cad.%20Bah%C3%A7eli%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DEFNE PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DOĞA PARK',
      'Cebesoy Sok. Kireçhane Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DO%C4%9EA%20PARK%2C%20SAHRAYICED%C4%B0D%2C%20Cebesoy%20Sok.%20Kire%C3%A7hane%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DOĞA PARK'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'DOSTLUK PARKI',
      'Mescitli Sok',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=DOSTLUK%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20Mescitli%20Sok%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'DOSTLUK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ERGUVAN PARKI',
      'Mümin Deresi Sok. Çelik Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ERGUVAN%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20M%C3%BCmin%20Deresi%20Sok.%20%C3%87elik%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ERGUVAN PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ESİN IŞIK PARKI',
      'Mengi Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ES%C4%B0N%20I%C5%9EIK%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20Mengi%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ESİN IŞIK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MENGÜ SOKAK PARKI',
      'Mengi Sok. Uçar Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MENG%C3%9C%20SOKAK%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20Mengi%20Sok.%20U%C3%A7ar%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MENGÜ SOKAK PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MİLLİ HAKİMİYET PARKI',
      'İnönü Cad. Çamlık Parkı Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=M%C4%B0LL%C4%B0%20HAK%C4%B0M%C4%B0YET%20PARKI%2C%20SAHRAYICED%C4%B0D%2C%20%C4%B0n%C3%B6n%C3%BC%20Cad.%20%C3%87aml%C4%B1k%20Park%C4%B1%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MİLLİ HAKİMİYET PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Sahrayıcedit mahallesi bulunamadı, 8 kayıt atlandı';
  END IF;

  -- ================================================================
  -- SAHRAYICEDİT (2 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Sahrayıcedit' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HİLTON OTELİ YANI',
      'Batman Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=H%C4%B0LTON%20OTEL%C4%B0%20YANI%2C%20SAHRAYICED%C4%B0T%2C%20Batman%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HİLTON OTELİ YANI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'ARAPGİRLİ CAMİ PARKI',
      'Halk Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=ARAPG%C4%B0RL%C4%B0%20CAM%C4%B0%20PARKI%2C%20SAHRAYICED%C4%B0T%2C%20Halk%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'ARAPGİRLİ CAMİ PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Sahrayıcedit mahallesi bulunamadı, 2 kayıt atlandı';
  END IF;

  -- ================================================================
  -- SUADİYE (4 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Suadiye' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SUADİYE İSTASYON PARKI',
      'Ayşe Çavuş Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SUAD%C4%B0YE%20%C4%B0STASYON%20PARKI%2C%20SUAD%C4%B0YE%2C%20Ay%C5%9Fe%20%C3%87avu%C5%9F%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SUADİYE İSTASYON PARKI'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'MEHMET KARAMANCI İLKOKULU',
      'Çamlı Sok. Kurudere Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=MEHMET%20KARAMANCI%20%C4%B0LKOKULU%2C%20SUAD%C4%B0YE%2C%20%C3%87aml%C4%B1%20Sok.%20Kurudere%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'MEHMET KARAMANCI İLKOKULU'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'HACI MUSTAFA TARMAN ANADOLU LİSESİ',
      'Emin Ali Paşa Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=HACI%20MUSTAFA%20TARMAN%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20SUAD%C4%B0YE%2C%20Emin%20Ali%20Pa%C5%9Fa%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'HACI MUSTAFA TARMAN ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SUADİYE CAMİİ',
      'Suadiye Camii Sok.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=SUAD%C4%B0YE%20CAM%C4%B0%C4%B0%2C%20SUAD%C4%B0YE%2C%20Suadiye%20Camii%20Sok.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SUADİYE CAMİİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Suadiye mahallesi bulunamadı, 4 kayıt atlandı';
  END IF;

  -- ================================================================
  -- ZÜHTÜPAŞA (3 kayıt)
  -- ================================================================
  SELECT id INTO nh FROM neighborhoods WHERE name = 'Zühtüpaşa' AND district_id = d_id;
  IF nh IS NOT NULL THEN
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      '23 NİSAN PARKI (1.BÖLGE)',
      'Hasan Kamil Sporel Sk.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=23%20N%C4%B0SAN%20PARKI%20%281.B%C3%96LGE%29%2C%20Z%C3%9CHT%C3%9CPA%C5%9EA%2C%20Hasan%20Kamil%20Sporel%20Sk.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = '23 NİSAN PARKI (1.BÖLGE)'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'İSTANBUL ANADOLU LİSESİ',
      'Recep Peker Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=%C4%B0STANBUL%20ANADOLU%20L%C4%B0SES%C4%B0%2C%20Z%C3%9CHT%C3%9CPA%C5%9EA%2C%20Recep%20Peker%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'İSTANBUL ANADOLU LİSESİ'
         AND neighborhood_id = nh
         AND source_name = src
    );
    INSERT INTO assembly_areas
      (neighborhood_id, district_id, name, address, latitude, longitude, google_maps_url, source_name, source_reference, needs_review, is_active, created_at)
    SELECT nh, d_id,
      'SÖĞÜTLÜÇEŞME MARMARAY İSTASYONU FENERBAHÇE STADI TERAFI YEŞİL ALAN',
      'Taşköprü Cad.',
      NULL, NULL,
      'https://www.google.com/maps/search/?api=1&query=S%C3%96%C4%9E%C3%9CTL%C3%9C%C3%87E%C5%9EME%20MARMARAY%20%C4%B0STASYONU%20FENERBAH%C3%87E%20STADI%20TERAFI%20YE%C5%9E%C4%B0L%20ALAN%2C%20Z%C3%9CHT%C3%9CPA%C5%9EA%2C%20Ta%C5%9Fk%C3%B6pr%C3%BC%20Cad.%2C%20Kad%C4%B1k%C3%B6y%2C%20%C4%B0stanbul',
      src, sref, FALSE, TRUE, now()
    WHERE NOT EXISTS (
      SELECT 1 FROM assembly_areas
       WHERE name = 'SÖĞÜTLÜÇEŞME MARMARAY İSTASYONU FENERBAHÇE STADI TERAFI YEŞİL ALAN'
         AND neighborhood_id = nh
         AND source_name = src
    );
  ELSE
    RAISE NOTICE 'Zühtüpaşa mahallesi bulunamadı, 3 kayıt atlandı';
  END IF;

END $$;
