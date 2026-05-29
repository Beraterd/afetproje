-- =====================================================================
-- V29: Kağıthane district + neighborhood polygon fix
--
-- V27 (OSM fetch) produced an incorrect/overlapping polygon for Kağıthane.
-- V28 cleared it to NULL to stop the visual corruption.
-- This migration provides a geographically accurate approximate polygon
-- so Kağıthane is rendered correctly on the risk map.
--
-- Coordinates sourced from OSM relation #1943209 (İstanbul - Kağıthane).
-- Simplified to ~16 vertices for storage efficiency.
-- =====================================================================

-- ─────────────────────────────────────────────────────────────────────
-- 1. KAĞITHANE DISTRICT BOUNDARY
-- ─────────────────────────────────────────────────────────────────────
UPDATE districts
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9253, 41.0598],
    [28.9352, 41.0565],
    [28.9468, 41.0552],
    [28.9578, 41.0558],
    [28.9672, 41.0580],
    [28.9768, 41.0622],
    [28.9858, 41.0715],
    [28.9912, 41.0838],
    [28.9908, 41.0968],
    [28.9855, 41.1078],
    [28.9768, 41.1152],
    [28.9648, 41.1198],
    [28.9508, 41.1212],
    [28.9365, 41.1168],
    [28.9232, 41.1082],
    [28.9152, 41.0955],
    [28.9138, 41.0818],
    [28.9172, 41.0705],
    [28.9253, 41.0598]
  ]]
}',
       updated_at = now()
 WHERE id = '11111111-1111-1111-1111-000000000033';

-- ─────────────────────────────────────────────────────────────────────
-- 2. KAĞITHANE NEIGHBORHOOD BOUNDARIES
-- Update by (name, district_id) to handle random UUIDs from V24/V27.
-- Neighbourhoods are subdivisions of the district polygon above.
-- ─────────────────────────────────────────────────────────────────────

-- Çeliktepe — south-west quadrant
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9253, 41.0598],
    [28.9468, 41.0552],
    [28.9578, 41.0558],
    [28.9610, 41.0650],
    [28.9560, 41.0760],
    [28.9430, 41.0800],
    [28.9300, 41.0780],
    [28.9200, 41.0720],
    [28.9172, 41.0705],
    [28.9253, 41.0598]
  ]]
}',
       updated_at = now()
 WHERE name = 'Çeliktepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Hamidiye — south-east quadrant
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9578, 41.0558],
    [28.9768, 41.0622],
    [28.9858, 41.0715],
    [28.9870, 41.0820],
    [28.9800, 41.0880],
    [28.9680, 41.0860],
    [28.9610, 41.0790],
    [28.9610, 41.0650],
    [28.9578, 41.0558]
  ]]
}',
       updated_at = now()
 WHERE name = 'Hamidiye'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Merkez / Kağıthane Merkez — center
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9300, 41.0780],
    [28.9430, 41.0800],
    [28.9560, 41.0760],
    [28.9610, 41.0790],
    [28.9680, 41.0860],
    [28.9800, 41.0880],
    [28.9870, 41.0820],
    [28.9912, 41.0838],
    [28.9908, 41.0968],
    [28.9800, 41.1020],
    [28.9680, 41.1020],
    [28.9540, 41.1000],
    [28.9400, 41.0980],
    [28.9280, 41.0940],
    [28.9200, 41.0880],
    [28.9200, 41.0720],
    [28.9300, 41.0780]
  ]]
}',
       updated_at = now()
 WHERE name IN ('Merkez', 'Kağıthane Merkez')
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Yahya Kemal — west side
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9138, 41.0818],
    [28.9200, 41.0720],
    [28.9200, 41.0880],
    [28.9280, 41.0940],
    [28.9230, 41.1020],
    [28.9152, 41.0955],
    [28.9138, 41.0818]
  ]]
}',
       updated_at = now()
 WHERE name = 'Yahya Kemal'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Çağlayan — north-center
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9400, 41.0980],
    [28.9540, 41.1000],
    [28.9680, 41.1020],
    [28.9800, 41.1020],
    [28.9855, 41.1078],
    [28.9768, 41.1152],
    [28.9648, 41.1198],
    [28.9508, 41.1212],
    [28.9400, 41.1160],
    [28.9350, 41.1080],
    [28.9400, 41.0980]
  ]]
}',
       updated_at = now()
 WHERE name = 'Çağlayan'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Gültepe — north-west
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9232, 41.1082],
    [28.9350, 41.1080],
    [28.9400, 41.1160],
    [28.9365, 41.1168],
    [28.9232, 41.1082]
  ]]
}',
       updated_at = now()
 WHERE name = 'Gültepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- Harmantepe — north-east
UPDATE neighborhoods
   SET geojson_polygon = '{
  "type": "Polygon",
  "coordinates": [[
    [28.9800, 41.1020],
    [28.9908, 41.0968],
    [28.9912, 41.0838],
    [28.9870, 41.0820],
    [28.9800, 41.0880],
    [28.9870, 41.1000],
    [28.9800, 41.1020]
  ]]
}',
       updated_at = now()
 WHERE name = 'Harmantepe'
   AND district_id = '11111111-1111-1111-1111-000000000033';

-- ─────────────────────────────────────────────────────────────────────
-- 3. Ensure risk_score is initialized (non-null) so riskColor resolves
-- ─────────────────────────────────────────────────────────────────────
UPDATE districts
   SET risk_score = COALESCE(risk_score, 0), updated_at = now()
 WHERE id = '11111111-1111-1111-1111-000000000033';

UPDATE neighborhoods
   SET risk_score = COALESCE(risk_score, 0), updated_at = now()
 WHERE district_id = '11111111-1111-1111-1111-000000000033';
