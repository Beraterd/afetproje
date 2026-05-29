-- =====================================================================
-- V37: Remove legacy placeholder / non-administrative neighborhoods
--      from the original 10 pilot districts (V5 + V6 seeds).
--
-- BACKGROUND
--   V5 seeded simplified placeholder neighborhoods like "Pendik Merkez",
--   "Kartal Merkez", "Moda", "Ataköy" etc. with fixed UUIDs.
--   V6 added more non-OSM names: "Soğanlık", "Topselvi", "Kocasinan",
--   "Şenlik", "Haseki", "Çarşamba", "Galata", "Taksim", "Tarlabaşı",
--   "Kasımpaşa" for the pilot districts.
--   V18 later inserted REAL OSM administrative boundaries using official
--   names (e.g. "Soğanlık Yeni", "Kocasinan Merkez", "Topselvi Mahalesi").
--   V22 nullified the polygons of the stale rows — but the rows themselves
--   remained in the DB, appearing in dropdowns alongside real neighborhoods.
--   V36 handled districts 12–39 (V24); this migration handles districts 1–10.
--
-- SAFETY RULES
--   GROUP A — No event / resource_request / damage_assessment references:
--             deleted directly (assembly_areas cascade first).
--   GROUP B — Referenced by V28 demo seed data (known fixed UUIDs):
--             demo events / resource_requests are cleaned up first.
--             User neighborhood assignments are remapped to a real OSM
--             neighborhood in the same district (NOT set to NULL, which
--             would violate the NOT NULL constraint and the
--             trg_validate_user_neighborhood trigger).
--   SKIPPED  — Neighborhoods with real polygon data (V18 matched) or
--             real administrative significance are left untouched.
--             ('Cihangir', 'Sultanahmet', 'Bostancı', 'Erenköy', …)
-- =====================================================================

BEGIN;

-- =====================================================================
-- GROUP A  —  Safe deletes (no event / RR / user references)
-- =====================================================================

-- ── A1: Pendik Merkez (Pendik, dist 1) ──────────────────────────────
-- No assembly areas, no events, no users → direct delete.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000002';   -- Pendik Merkez

-- ── A2: Ataşehir Merkez (Ataşehir, dist 5) ──────────────────────────
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000000b';   -- Ataşehir Merkez

-- ── A3: Yenibosna (Bahçelievler, dist 6) — V5 non-OSM version ───────
-- V18 inserted 'Yenibosna Merkez' (real OSM). V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000000d';   -- Yenibosna

-- ── A4: Soğanlık (Kartal, dist 2) — V6 non-OSM version ──────────────
-- V18 inserted 'Soğanlık Yeni' (real OSM). V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000104';   -- Soğanlık (Kartal)

-- ── A5: Topselvi (Kartal, dist 2) — V6 non-OSM version ──────────────
-- V18 inserted 'Topselvi Mahalesi' (real OSM). V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000105';   -- Topselvi (Kartal)

-- ── A6: Kocasinan (Bahçelievler, dist 6) — V6 non-OSM version ───────
-- V18 inserted 'Kocasinan Merkez' (real OSM). V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000111';   -- Kocasinan (Bahçelievler)

-- ── A7: Şenlik (Bakırköy, dist 8) — V6 non-OSM version ──────────────
-- V18 inserted real Bakırköy boundaries. V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000118';   -- Şenlik (Bakırköy)

-- ── A8: Haseki (Fatih, dist 9) — V6 non-OSM version ─────────────────
-- V18 inserted 'Haseki Sultan'. V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000011a';   -- Haseki (Fatih)

-- ── A9: Çarşamba (Fatih, dist 9) — V6 non-OSM version ───────────────
-- V18 inserted 'Karagümrük' for this area. V22 nullified polygon.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000011b';   -- Çarşamba (Fatih)

-- ── A10: Galata (Beyoğlu, dist 10) — V6 non-OSM version ─────────────
-- Has one assembly area seeded in V6. Delete that first.
-- V18 inserted 'Kemankeş Karamustafa Paşa' / 'Şehit Muhtar' for this area.
DELETE FROM assembly_areas
 WHERE neighborhood_id = '22222222-2222-2222-2222-000000000015';  -- Galata assembly area

DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000015';   -- Galata (Beyoğlu)

-- ── A11: Taksim (Beyoğlu, dist 10) — V6 non-OSM version ─────────────
-- Has 'Taksim Meydanı Toplanma Alanı' assembly area seeded in V6.
DELETE FROM assembly_areas
 WHERE neighborhood_id = '22222222-2222-2222-2222-00000000011c';

DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000011c';   -- Taksim (Beyoğlu)

-- ── A12: Tarlabaşı (Beyoğlu, dist 10) — V6 non-OSM version ──────────
-- V22 nullified polygon. No assembly areas, no event/RR references.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000011d';   -- Tarlabaşı (Beyoğlu)

-- ── A13: Kasımpaşa (Beyoğlu, dist 10) — V6 non-OSM version ──────────
-- V22 nullified polygon. No references.
DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-00000000011e';   -- Kasımpaşa (Beyoğlu)


-- =====================================================================
-- GROUP B  —  Cascade delete of V28 demo data, then delete neighborhood
-- =====================================================================
-- All event/RR IDs here are the deterministic V28 demo seeds.
-- event_volunteers is cleaned first (no-op in fresh DB, safe in others).
--
-- NOTE: User neighborhood assignments are remapped to the first real
-- OSM neighborhood in the same district (alphabetical order) instead
-- of being set to NULL. Setting NULL would violate the NOT NULL
-- constraint on users.neighborhood_id AND would trigger the
-- trg_validate_user_neighborhood trigger exception.

-- ── B1: Moda (Kadıköy, dist 4) ───────────────────────────────────────
-- References: user 00002 (Ahmet Yılmaz), event ee01, RR aa01, assembly area

-- event_volunteers for event ee01
DELETE FROM event_volunteers
 WHERE event_id = 'eeeeeeee-0000-0000-0000-000000000001';

-- resource_request aa01
DELETE FROM resource_requests
 WHERE id = 'aaaaaaaa-0000-0000-0000-000000000001';

-- event ee01
DELETE FROM events
 WHERE id = 'eeeeeeee-0000-0000-0000-000000000001';

 -- Remap damage assessments from placeholder neighborhood to a real neighborhood in same district
UPDATE damage_assessments da
   SET neighborhood_id = (
       SELECT n.id
         FROM neighborhoods n
        WHERE n.district_id = da.district_id
          AND n.id <> '22222222-2222-2222-2222-000000000008'
        ORDER BY n.name
        LIMIT 1
   )
 WHERE da.neighborhood_id = '22222222-2222-2222-2222-000000000008';

-- Remap user 00002 to first real OSM neighborhood in Kadıköy (dist 4)
UPDATE users
   SET neighborhood_id = (
       SELECT n.id
         FROM neighborhoods n
        WHERE n.district_id = (
            SELECT district_id FROM users WHERE id = '00000000-0000-0000-0000-000000000002'
        )
          AND n.id <> '22222222-2222-2222-2222-000000000008'
        ORDER BY n.name
        LIMIT 1
   ),
   updated_at = now()
 WHERE id = '00000000-0000-0000-0000-000000000002'
   AND neighborhood_id = '22222222-2222-2222-2222-000000000008';

-- assembly area
DELETE FROM assembly_areas
 WHERE neighborhood_id = '22222222-2222-2222-2222-000000000008';

DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000008';   -- Moda (Kadıköy)


-- ── B2: Kartal Merkez (Kartal, dist 2) ───────────────────────────────
-- References: user 00007 (Elif Çelik), event ee06, RR aa06,
--             assembly area 'Kartal Sahil Toplanma Alanı'

-- event_volunteers for ee06
DELETE FROM event_volunteers
 WHERE event_id = 'eeeeeeee-0000-0000-0000-000000000006';

-- resource_request aa06
DELETE FROM resource_requests
 WHERE id = 'aaaaaaaa-0000-0000-0000-000000000006';

-- event ee06
DELETE FROM events
 WHERE id = 'eeeeeeee-0000-0000-0000-000000000006';

-- Remap user 00007 to first real OSM neighborhood in Kartal (dist 2)
UPDATE users
   SET neighborhood_id = (
       SELECT n.id
         FROM neighborhoods n
        WHERE n.district_id = (
            SELECT district_id FROM users WHERE id = '00000000-0000-0000-0000-000000000007'
        )
          AND n.id <> '22222222-2222-2222-2222-000000000004'
        ORDER BY n.name
        LIMIT 1
   ),
   updated_at = now()
 WHERE id = '00000000-0000-0000-0000-000000000007'
   AND neighborhood_id = '22222222-2222-2222-2222-000000000004';

-- assembly area (Kartal Sahil)
DELETE FROM assembly_areas
 WHERE neighborhood_id = '22222222-2222-2222-2222-000000000004';

DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000004';   -- Kartal Merkez


-- ── B3: Tuzla Merkez (Tuzla, dist 3) ─────────────────────────────────
-- References: RR aa07 only. No assembly areas, no users.

-- resource_request aa07
DELETE FROM resource_requests
 WHERE id = 'aaaaaaaa-0000-0000-0000-000000000007';

DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000006';   -- Tuzla Merkez


-- ── B4: Ataköy (Bakırköy, dist 8) ────────────────────────────────────
-- References: user 00006 (Ali Şahin), event ee05, RR aa05,
--             damage_assessments (V17: 3 records)
-- Note: V22 nullified polygon. V18 inserted real Bakırköy neighborhoods.

-- event_volunteers for ee05
DELETE FROM event_volunteers
 WHERE event_id = 'eeeeeeee-0000-0000-0000-000000000005';

-- resource_request aa05
DELETE FROM resource_requests
 WHERE id = 'aaaaaaaa-0000-0000-0000-000000000005';

-- event ee05
DELETE FROM events
 WHERE id = 'eeeeeeee-0000-0000-0000-000000000005';

-- Remap damage_assessments (V17 seeded 3 records for Ataköy) to first
-- real OSM neighborhood in Bakırköy.
-- damage_assessments.neighborhood_id is NOT NULL with no ON DELETE action
-- → FK violation if not remapped before neighborhood delete.
UPDATE damage_assessments da
   SET neighborhood_id = (
       SELECT n.id
         FROM neighborhoods n
        WHERE n.district_id = da.district_id
          AND n.id <> '22222222-2222-2222-2222-000000000011'
        ORDER BY n.name
        LIMIT 1
   )
 WHERE da.neighborhood_id = '22222222-2222-2222-2222-000000000011';

-- Remap user 00006 to first real OSM neighborhood in Bakırköy (dist 8)
UPDATE users
   SET neighborhood_id = (
       SELECT n.id
         FROM neighborhoods n
        WHERE n.district_id = (
            SELECT district_id FROM users WHERE id = '00000000-0000-0000-0000-000000000006'
        )
          AND n.id <> '22222222-2222-2222-2222-000000000011'
        ORDER BY n.name
        LIMIT 1
   ),
   updated_at = now()
 WHERE id = '00000000-0000-0000-0000-000000000006'
   AND neighborhood_id = '22222222-2222-2222-2222-000000000011';

-- No assembly areas for Ataköy (22222222-000011) in V5/V6 (V5 uses
-- Kartal Sahil for neighborhood 000004, not Ataköy 000011).
DELETE FROM assembly_areas
 WHERE neighborhood_id = '22222222-2222-2222-2222-000000000011';

DELETE FROM neighborhoods
 WHERE id = '22222222-2222-2222-2222-000000000011';   -- Ataköy (Bakırköy)


-- =====================================================================
-- SKIPPED (not deleted — retain for data integrity)
-- =====================================================================
-- 'Sultanahmet' (22222222-000013, Fatih): V28 event ee04 / RR aa04 /
--   user 00005 reference it. 'Sultanahmet' is also a real and well-known
--   administrative name. The polygon was nullified by V22; V18 added
--   'Sultan Ahmet' as the OSM official row. Left intact.
-- 'Cihangir' (22222222-000016, Beyoğlu): V18 matched it correctly —
--   polygon is NOT null. Real neighborhood. Left intact.
-- 'Bostancı' (22222222-000009, Kadıköy): real neighborhood, V28 event ee07.
-- 'Erenköy' (22222222-00000a, Kadıköy): real neighborhood.
-- 'Zeytinlik' (22222222-000012, Bakırköy): real neighborhood.
-- 'Uğur Mumcu' (22222222-000005, Kartal): real neighborhood.
-- 'Hürriyet' (22222222-000106, Kartal): real neighborhood added by V6.

COMMIT;
