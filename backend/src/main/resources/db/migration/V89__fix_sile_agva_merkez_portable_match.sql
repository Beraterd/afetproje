-- V89: Fix a non-portable data reference in V88.
--
-- V88's Şile "Ağva" -> "Ağva Merkez" rename/geometry-backfill matched the
-- target row by a hardcoded neighborhood UUID
-- ('1618a471-d7d4-4ef8-acdb-c0abc63ff3d5') read off one specific already-
-- running dev database. That row's id was assigned by gen_random_uuid()
-- when V27 originally inserted it — it is NOT deterministic across
-- environments. Verified on a disposable, from-scratch Postgres instance
-- (V1..V88 applied, nothing else): that UUID does not exist there, so
-- V88's UPDATE silently matched zero rows and Şile's "Ağva Merkez" mahalle
-- was left un-backfilled on any environment that didn't happen to already
-- have this session's dev DB history — including a fresh checkout.
--
-- Fix: match by (district_id, name) instead of a hardcoded row id — both
-- of those ARE deterministic (Şile's district_id is the fixed constant
-- used throughout this codebase; 'Ağva' is the exact name V27's OSM import
-- would have used for this mahalle before V88 renamed it). Idempotent by
-- construction: on a database where V88 already renamed the row to
-- "Ağva Merkez" (e.g. this project's own dev DB), the WHERE clause below
-- (name = 'Ağva') matches zero rows and this is a no-op; on a database
-- where the row is still named "Ağva" (a genuinely fresh V1..V88 apply),
-- it performs the same rename + geometry backfill V88 intended.
--
-- Same evidence/source as V88: fresh OSM Overpass fetch (cached at
-- scripts/.audit_cache/sile_ground_truth.json) + independent confirmation
-- via Turkey's official TÜİK/İçişleri Bakanlığı neighborhood list
-- (github.com/bertugfahriozer/il_ilce_mahalle) that "Ağva Merkez" is
-- Şile's real, currently-registered mahalle name.

BEGIN;

UPDATE neighborhoods n
   SET name = 'Ağva Merkez',
       geojson_polygon = '{"type":"Polygon","coordinates":[[[29.8476392,41.1389928],[29.849791,41.1389288],[29.8504705,41.139174],[29.850696296775272,41.13928883509055],[29.851051,41.1394299],[29.8515606,41.1397284],[29.8518154,41.1399736],[29.8519287,41.1402935],[29.8512264,41.140768],[29.8512264,41.1408815],[29.8514073,41.1409496],[29.8534369,41.1395573],[29.8535819,41.1397315],[29.8546226,41.1393303],[29.8549742,41.1392924],[29.8553413,41.1393114],[29.855352613810624,41.1394667998113],[29.8575483,41.1394368],[29.859027,41.1390378],[29.8611156,41.1370471],[29.8613867,41.1359984],[29.8609616,41.1353302],[29.8580782,41.1341191],[29.8563715,41.1331121],[29.8539809,41.1304205],[29.8539871,41.1296502],[29.8543445,41.1289262],[29.8555158,41.1279034],[29.8544887,41.1277662],[29.8535123,41.1274753],[29.8526165,41.1270551],[29.8504943,41.1248172],[29.8501677,41.1247081],[29.8500753,41.124548],[29.8497826,41.1244064],[29.8492976,41.1243772],[29.8494713,41.1252121],[29.8493862,41.1255551],[29.8492241,41.1257603],[29.8458043,41.1281002],[29.8437307,41.1289163],[29.8415756,41.1305985],[29.8407745,41.1314906],[29.8399103,41.1326719],[29.8397874,41.1332143],[29.8398166,41.1337007],[29.8401022,41.1343953],[29.8435633,41.137945],[29.8445877,41.1382328],[29.846321,41.1381634],[29.8473772,41.1387225],[29.8476392,41.1389928]]]}'::jsonb,
       updated_at = now()
  FROM districts d
 WHERE n.district_id = d.id
   AND d.id = '11111111-1111-1111-1111-000000000017'  -- Şile (fixed constant, see V25/fetch_istanbul_boundaries.py)
   AND n.name = 'Ağva';

COMMIT;
