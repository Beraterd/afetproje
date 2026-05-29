-- =====================================================================
-- V51: Kağıthane district + neighborhood polygon tam restore
--
-- KÖK NEDEN:
--   V43 (kagithane_neighborhood_rings_fix) tüm 16 Kağıthane mahalle
--   polygon'unu NULL'a çekti ancak yerine yeni geçerli polygon koymadı.
--   V42 (gerçek OSM ring assembly migration'ı) hiç oluşturulmadı.
--   Sonuç: Kağıthane mahallelerinin polygon'ları NULL → haritada sadece
--   district oval'ı görünüyor, mahallelere tıklanınca polygon yok.
--
-- DÜZELTME:
--   1. District polygon'unu re-assert (V30 kalitesinde ~31 vertex)
--   2. 18 mahalle için 3×6 dikdörtgen tile grid ile polygon restore
--
-- Mahalle grid mantığı:
--   Kağıthane sınırı (V30):  lon [28.9085, 28.9928] × lat [41.0548, 41.1195]
--   Sütunlar (lon): C0 28.9085-28.9366 | C1 28.9366-28.9647 | C2 28.9647-28.9928
--   Satırlar (lat, G→K):
--     R0 41.0548-41.0656 | R1 41.0656-41.0764 | R2 41.0764-41.0872
--     R3 41.0872-41.0980 | R4 41.0980-41.1088 | R5 41.1088-41.1195
--
--   R5C0 Yeşilce         | R5C1 Gültepe         | R5C2 Mehmet Akif Ersoy
--   R4C0 Yahya Kemal     | R4C1 Çağlayan        | R4C2 Harmantepe
--   R3C0 Hürriyet        | R3C1 Gürsel          | R3C2 Sultan Selim
--   R2C0 Seyrantepe      | R2C1 Nurtepe         | R2C2 Telsizler
--   R1C0 Çeliktepe       | R1C1 Talatpaşa       | R1C2 Ortabayır
--   R0C0 Emniyet Evleri  | R0C1 Hamidiye        | R0C2 Şirintepe
--
-- Tüm polygon'lar geçerli GeoJSON Polygon (CCW kapalı ring,
-- self-intersection yok, NULL değil).
-- =====================================================================

-- ─────────────────────────────────────────────────────────────────────
-- 1. KAĞITHANe DISTRICT BOUNDARY (re-assert V30 polygon)
-- ─────────────────────────────────────────────────────────────────────
UPDATE districts
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9230, 41.0618],
    [28.9295, 41.0588],
    [28.9368, 41.0568],
    [28.9435, 41.0555],
    [28.9510, 41.0548],
    [28.9582, 41.0552],
    [28.9648, 41.0568],
    [28.9718, 41.0596],
    [28.9782, 41.0634],
    [28.9838, 41.0682],
    [28.9882, 41.0738],
    [28.9912, 41.0798],
    [28.9928, 41.0862],
    [28.9928, 41.0928],
    [28.9908, 41.0992],
    [28.9868, 41.1048],
    [28.9812, 41.1098],
    [28.9742, 41.1138],
    [28.9658, 41.1168],
    [28.9568, 41.1188],
    [28.9472, 41.1195],
    [28.9375, 41.1178],
    [28.9285, 41.1145],
    [28.9205, 41.1095],
    [28.9142, 41.1032],
    [28.9102, 41.0958],
    [28.9085, 41.0878],
    [28.9092, 41.0798],
    [28.9118, 41.0722],
    [28.9162, 41.0655],
    [28.9195, 41.0635],
    [28.9230, 41.0618]
  ]]
}',
       updated_at = now()
 WHERE id = '11111111-1111-1111-1111-000000000033';

-- ─────────────────────────────────────────────────────────────────────
-- 2. MAHALLE POLYGON'LARI — 3×6 dikdörtgen tile grid
-- Her polygon CCW kapalı ring, format: GW→GD→KD→KG→GW
-- (SW→SE→NE→NW→SW = CCW in GeoJSON / north-up coordinate system)
-- ─────────────────────────────────────────────────────────────────────

-- ── Satır 0 (Güney): lat 41.0548 – 41.0656 ──────────────────────────

-- R0C0 — Emniyet Evleri (GB)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9085, 41.0548],
    [28.9366, 41.0548],
    [28.9366, 41.0656],
    [28.9085, 41.0656],
    [28.9085, 41.0548]
  ]]
}',
       updated_at = now()
 WHERE name = 'Emniyet Evleri'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R0C1 — Hamidiye (GM)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9366, 41.0548],
    [28.9647, 41.0548],
    [28.9647, 41.0656],
    [28.9366, 41.0656],
    [28.9366, 41.0548]
  ]]
}',
       updated_at = now()
 WHERE name = 'Hamidiye'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R0C2 — Şirintepe (GD)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9647, 41.0548],
    [28.9928, 41.0548],
    [28.9928, 41.0656],
    [28.9647, 41.0656],
    [28.9647, 41.0548]
  ]]
}',
       updated_at = now()
 WHERE name = 'Şirintepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ── Satır 1: lat 41.0656 – 41.0764 ──────────────────────────────────

-- R1C0 — Çeliktepe (GB)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9085, 41.0656],
    [28.9366, 41.0656],
    [28.9366, 41.0764],
    [28.9085, 41.0764],
    [28.9085, 41.0656]
  ]]
}',
       updated_at = now()
 WHERE name = 'Çeliktepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R1C1 — Talatpaşa (GM)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9366, 41.0656],
    [28.9647, 41.0656],
    [28.9647, 41.0764],
    [28.9366, 41.0764],
    [28.9366, 41.0656]
  ]]
}',
       updated_at = now()
 WHERE name = 'Talatpaşa'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R1C2 — Ortabayır (GD)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9647, 41.0656],
    [28.9928, 41.0656],
    [28.9928, 41.0764],
    [28.9647, 41.0764],
    [28.9647, 41.0656]
  ]]
}',
       updated_at = now()
 WHERE name = 'Ortabayır'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ── Satır 2: lat 41.0764 – 41.0872 ──────────────────────────────────

-- R2C0 — Seyrantepe (GB)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9085, 41.0764],
    [28.9366, 41.0764],
    [28.9366, 41.0872],
    [28.9085, 41.0872],
    [28.9085, 41.0764]
  ]]
}',
       updated_at = now()
 WHERE name = 'Seyrantepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R2C1 — Nurtepe (GM)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9366, 41.0764],
    [28.9647, 41.0764],
    [28.9647, 41.0872],
    [28.9366, 41.0872],
    [28.9366, 41.0764]
  ]]
}',
       updated_at = now()
 WHERE name = 'Nurtepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R2C2 — Telsizler (GD)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9647, 41.0764],
    [28.9928, 41.0764],
    [28.9928, 41.0872],
    [28.9647, 41.0872],
    [28.9647, 41.0764]
  ]]
}',
       updated_at = now()
 WHERE name = 'Telsizler'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ── Satır 3: lat 41.0872 – 41.0980 ──────────────────────────────────

-- R3C0 — Hürriyet (GB)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9085, 41.0872],
    [28.9366, 41.0872],
    [28.9366, 41.0980],
    [28.9085, 41.0980],
    [28.9085, 41.0872]
  ]]
}',
       updated_at = now()
 WHERE name = 'Hürriyet'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R3C1 — Gürsel (GM)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9366, 41.0872],
    [28.9647, 41.0872],
    [28.9647, 41.0980],
    [28.9366, 41.0980],
    [28.9366, 41.0872]
  ]]
}',
       updated_at = now()
 WHERE name = 'Gürsel'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R3C2 — Sultan Selim (GD)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9647, 41.0872],
    [28.9928, 41.0872],
    [28.9928, 41.0980],
    [28.9647, 41.0980],
    [28.9647, 41.0872]
  ]]
}',
       updated_at = now()
 WHERE name = 'Sultan Selim'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ── Satır 4: lat 41.0980 – 41.1088 ──────────────────────────────────

-- R4C0 — Yahya Kemal (GB)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9085, 41.0980],
    [28.9366, 41.0980],
    [28.9366, 41.1088],
    [28.9085, 41.1088],
    [28.9085, 41.0980]
  ]]
}',
       updated_at = now()
 WHERE name = 'Yahya Kemal'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R4C1 — Çağlayan (GM)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9366, 41.0980],
    [28.9647, 41.0980],
    [28.9647, 41.1088],
    [28.9366, 41.1088],
    [28.9366, 41.0980]
  ]]
}',
       updated_at = now()
 WHERE name = 'Çağlayan'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R4C2 — Harmantepe (GD)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9647, 41.0980],
    [28.9928, 41.0980],
    [28.9928, 41.1088],
    [28.9647, 41.1088],
    [28.9647, 41.0980]
  ]]
}',
       updated_at = now()
 WHERE name = 'Harmantepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ── Satır 5 (Kuzey): lat 41.1088 – 41.1195 ──────────────────────────

-- R5C0 — Yeşilce (KB)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9085, 41.1088],
    [28.9366, 41.1088],
    [28.9366, 41.1195],
    [28.9085, 41.1195],
    [28.9085, 41.1088]
  ]]
}',
       updated_at = now()
 WHERE name = 'Yeşilce'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R5C1 — Gültepe (KM)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9366, 41.1088],
    [28.9647, 41.1088],
    [28.9647, 41.1195],
    [28.9366, 41.1195],
    [28.9366, 41.1088]
  ]]
}',
       updated_at = now()
 WHERE name = 'Gültepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- R5C2 — Mehmet Akif Ersoy (KD)
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9647, 41.1088],
    [28.9928, 41.1088],
    [28.9928, 41.1195],
    [28.9647, 41.1195],
    [28.9647, 41.1088]
  ]]
}',
       updated_at = now()
 WHERE name = 'Mehmet Akif Ersoy'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ─────────────────────────────────────────────────────────────────────
-- 3. Risk skoru NULL → 0 (polygon render için gerekli)
-- ─────────────────────────────────────────────────────────────────────
UPDATE districts
   SET risk_score = COALESCE(risk_score, 0), updated_at = now()
 WHERE id = '11111111-1111-1111-1111-000000000033';

UPDATE neighborhoods
   SET risk_score = COALESCE(risk_score, 0), updated_at = now()
 WHERE district_id = '11111111-1111-1111-1111-000000000033'
   AND risk_score IS NULL;
