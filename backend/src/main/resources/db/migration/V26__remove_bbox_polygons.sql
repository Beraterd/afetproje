-- V26: Remove inaccurate bounding-box district polygons added in V25.
-- Real GeoJSON boundaries for all 39 districts will be fetched from
-- OpenStreetMap Overpass API by running:
--
--   pip install requests shapely
--   python scripts/fetch_istanbul_boundaries.py
--
-- That script outputs V27__osm_all39_boundaries.sql with real OSM data
-- (same quality/method as V10 which covers the original 10 pilot districts).
-- Run it before starting the backend so V27 is applied by Flyway.

UPDATE districts
SET geojson_polygon = NULL, updated_at = now()
WHERE id IN (
    -- 29 new districts added in V24 (V25 gave them bounding-box approximations)
    '11111111-1111-1111-1111-000000000011', -- Adalar
    '11111111-1111-1111-1111-000000000012', -- Beykoz
    '11111111-1111-1111-1111-000000000013', -- Çekmeköy
    '11111111-1111-1111-1111-000000000014', -- Maltepe
    '11111111-1111-1111-1111-000000000015', -- Sancaktepe
    '11111111-1111-1111-1111-000000000016', -- Sultanbeyli
    '11111111-1111-1111-1111-000000000017', -- Şile
    '11111111-1111-1111-1111-000000000018', -- Ümraniye
    '11111111-1111-1111-1111-000000000019', -- Üsküdar
    '11111111-1111-1111-1111-000000000020', -- Arnavutköy
    '11111111-1111-1111-1111-000000000021', -- Avcılar
    '11111111-1111-1111-1111-000000000022', -- Bağcılar
    '11111111-1111-1111-1111-000000000023', -- Başakşehir
    '11111111-1111-1111-1111-000000000024', -- Bayrampaşa
    '11111111-1111-1111-1111-000000000025', -- Beylikdüzü
    '11111111-1111-1111-1111-000000000026', -- Büyükçekmece
    '11111111-1111-1111-1111-000000000027', -- Çatalca
    '11111111-1111-1111-1111-000000000028', -- Esenler
    '11111111-1111-1111-1111-000000000029', -- Esenyurt
    '11111111-1111-1111-1111-000000000030', -- Eyüpsultan
    '11111111-1111-1111-1111-000000000031', -- Gaziosmanpaşa
    '11111111-1111-1111-1111-000000000032', -- Güngören
    '11111111-1111-1111-1111-000000000033', -- Kağıthane
    '11111111-1111-1111-1111-000000000034', -- Küçükçekmece
    '11111111-1111-1111-1111-000000000035', -- Sarıyer
    '11111111-1111-1111-1111-000000000036', -- Silivri
    '11111111-1111-1111-1111-000000000037', -- Sultangazi
    '11111111-1111-1111-1111-000000000038', -- Şişli
    '11111111-1111-1111-1111-000000000039'  -- Zeytinburnu
);
