-- =====================================================================
-- V22: Clear stale/approximate neighborhood polygon data
--
-- PROBLEM:
--   V5 and V6 seeded simplified neighborhood names (e.g. "Kartal Merkez",
--   "Pendik Merkez", "Moda") with fixed UUIDs.
--   V9 then set APPROXIMATE (~1 km accuracy) polygons for these rows
--   using rough bounding-box coordinates.
--
--   V18 later inserted the REAL OSM neighborhood boundaries, but used
--   the actual administrative names (e.g. "Soğanlık Yeni", "Ataköy 1. Kısım",
--   "Sultan Ahmet"). Because V18 uses ON CONFLICT (name, district_id),
--   the old rows with DIFFERENT names were NOT updated — only new rows
--   were inserted alongside them.
--
-- RESULT:
--   Both the old V9 approximate polygon AND the new V18 OSM polygon
--   were rendered simultaneously on the map, causing:
--     - Visual overlaps (eski + yeni veri üst üste)
--     - Polygons extending into the sea (denize taşan sınırlar)
--     - Incorrect / outdated boundary shapes
--
-- FIX:
--   NULL out geojson_polygon for the stale rows whose names were NOT
--   covered by V18. The rows remain intact (events, users, assembly areas
--   linked to them are preserved). They simply stop rendering a polygon.
-- =====================================================================

BEGIN;

-- ── Pendik (000000000001) ─────────────────────────────────────────────
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Pendik Merkez'
   AND district_id = '11111111-1111-1111-1111-000000000001';

-- ── Kartal (000000000002) ─────────────────────────────────────────────
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name IN ('Kartal Merkez', 'Soğanlık', 'Topselvi')
   AND district_id = '11111111-1111-1111-1111-000000000002';
-- Note: V18 inserted 'Soğanlık Yeni' and 'Topselvi Mahalesi' as separate rows.

-- ── Tuzla (000000000003) ──────────────────────────────────────────────
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Tuzla Merkez'
   AND district_id = '11111111-1111-1111-1111-000000000003';

-- ── Kadıköy (000000000004) ────────────────────────────────────────────
-- 'Moda' is not a separate administrative neighborhood; the area is
-- covered by Caferağa / Osmanağa in OSM administrative data.
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Moda'
   AND district_id = '11111111-1111-1111-1111-000000000004';

-- ── Ataşehir (000000000005) ───────────────────────────────────────────
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name = 'Ataşehir Merkez'
   AND district_id = '11111111-1111-1111-1111-000000000005';

-- ── Bahçelievler (000000000006) ───────────────────────────────────────
-- V18 inserted 'Yenibosna Merkez' and 'Kocasinan Merkez' (OSM admin names).
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name IN ('Yenibosna', 'Kocasinan')
   AND district_id = '11111111-1111-1111-1111-000000000006';

-- ── Bakırköy (000000000008) ───────────────────────────────────────────
-- V18 inserted 'Ataköy 1. Kısım', '2-5-6. Kısım', '3-4-11. Kısım',
-- '7-8-9-10. Kısım' for the Ataköy blocks, and 'Şenlikköy' for Şenlik.
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name IN ('Ataköy', 'Şenlik')
   AND district_id = '11111111-1111-1111-1111-000000000008';

-- ── Fatih (000000000009) ──────────────────────────────────────────────
-- V18 uses official names: 'Sultan Ahmet', 'Haseki Sultan', 'Karagümrük'.
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name IN ('Sultanahmet', 'Haseki', 'Çarşamba')
   AND district_id = '11111111-1111-1111-1111-000000000009';

-- ── Beyoğlu (000000000010) ────────────────────────────────────────────
-- V18 uses granular OSM names ('Kemankeş Karamustafa Paşa', 'Şehit Muhtar',
-- 'Kaptanpaşa', etc.) instead of the historic quarter names.
-- 'Cihangir' was correctly matched and updated by V18.
UPDATE neighborhoods
   SET geojson_polygon = NULL, updated_at = now()
 WHERE name IN ('Galata', 'Taksim', 'Tarlabaşı', 'Kasımpaşa')
   AND district_id = '11111111-1111-1111-1111-000000000010';

COMMIT;
