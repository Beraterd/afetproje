-- =====================================================================
-- V43: Kağıthane mahalle ring assembly düzeltmesi
--
-- V42'deki sorun: relation_to_polygon() OSM way'lerini naif
-- concatenation ile birleştiriyordu → self-intersecting ring →
-- Leaflet SVG evenodd fill'de üçgen/parça render görünümü.
--
-- Düzeltme: assemble_rings() way'leri endpoint matching + reversal
-- ile zincirler → düzgün kapalı, non-self-intersecting ring.
-- Kaynak: OSM admin_level=8, Kağıthane (içinde district #1765894)
-- =====================================================================

-- NOT: V42 district polygon'u doğruydu, bu migration sadece
-- neighborhood polygon'larını yeniden yazar.

-- Eşleştirilemeyen mahalleler → NULL
-- (Yanlış polygon render etmekten daha iyi)
-- NULL: Çağlayan
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Çağlayan'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Çeliktepe
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Çeliktepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Emniyet Evleri
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Emniyet Evleri'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Gültepe
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Gültepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Gürsel
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Gürsel'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Hamidiye
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Hamidiye'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Harmantepe
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Harmantepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Hürriyet
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Hürriyet'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Mehmet Akif Ersoy
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Mehmet Akif Ersoy'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Ortabayır
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Ortabayır'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Sultan Selim
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Sultan Selim'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Şirintepe
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Şirintepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Talatpaşa
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Talatpaşa'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Telsizler
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Telsizler'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Yahya Kemal
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Yahya Kemal'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- NULL: Yeşilce
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Yeşilce'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Risk skoru NULL → 0
UPDATE neighborhoods
   SET risk_score = COALESCE(risk_score, 0), updated_at = now()
 WHERE district_id = '11111111-1111-1111-1111-000000000033' AND risk_score IS NULL;