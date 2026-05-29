-- V25: Approximate GeoJSON bounding-box polygons for the 29 new districts
-- Coordinates based on official Istanbul district boundaries (approximate rectangles).
-- Real OSM precision polygons can be imported later via the admin panel.
-- NOTE: Coastal districts are clipped to avoid sea overlap where possible.

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.045,40.830],[29.155,40.830],[29.155,40.930],[29.045,40.930],[29.045,40.830]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000011'; -- Adalar

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.050,41.060],[29.250,41.060],[29.250,41.230],[29.050,41.230],[29.050,41.060]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000012'; -- Beykoz

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.150,40.990],[29.280,40.990],[29.280,41.100],[29.150,41.100],[29.150,40.990]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000013'; -- Çekmeköy

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.090,40.895],[29.210,40.895],[29.210,40.960],[29.090,40.960],[29.090,40.895]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000014'; -- Maltepe

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.180,40.975],[29.320,40.975],[29.320,41.075],[29.180,41.075],[29.180,40.975]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000015'; -- Sancaktepe

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.200,40.940],[29.300,40.940],[29.300,41.005],[29.200,41.005],[29.200,40.940]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000016'; -- Sultanbeyli

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.350,41.050],[29.650,41.050],[29.650,41.380],[29.350,41.380],[29.350,41.050]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000017'; -- Şile

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.095,40.995],[29.200,40.995],[29.200,41.075],[29.095,41.075],[29.095,40.995]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000018'; -- Ümraniye

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.990,40.970],[29.110,40.970],[29.110,41.060],[28.990,41.060],[28.990,40.970]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000019'; -- Üsküdar

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.650,41.165],[28.850,41.165],[28.850,41.310],[28.650,41.310],[28.650,41.165]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000020'; -- Arnavutköy

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.685,40.965],[28.795,40.965],[28.795,41.025],[28.685,41.025],[28.685,40.965]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000021'; -- Avcılar

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.800,41.015],[28.900,41.015],[28.900,41.085],[28.800,41.085],[28.800,41.015]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000022'; -- Bağcılar

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.775,41.055],[28.940,41.055],[28.940,41.150],[28.775,41.150],[28.775,41.055]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000023'; -- Başakşehir

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.880,41.040],[28.940,41.040],[28.940,41.080],[28.880,41.080],[28.880,41.040]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000024'; -- Bayrampaşa

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.580,40.955],[28.720,40.955],[28.720,41.015],[28.580,41.015],[28.580,40.955]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000025'; -- Beylikdüzü

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.545,40.985],[28.720,40.985],[28.720,41.130],[28.545,41.130],[28.545,40.985]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000026'; -- Büyükçekmece

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[27.940,41.090],[28.650,41.090],[28.650,41.600],[27.940,41.600],[27.940,41.090]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000027'; -- Çatalca

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.840,41.015],[28.920,41.015],[28.920,41.075],[28.840,41.075],[28.840,41.015]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000028'; -- Esenler

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.640,41.005],[28.755,41.005],[28.755,41.080],[28.640,41.080],[28.640,41.005]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000029'; -- Esenyurt

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.880,41.055],[29.000,41.055],[29.000,41.220],[28.880,41.220],[28.880,41.055]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000030'; -- Eyüpsultan

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.875,41.060],[28.980,41.060],[28.980,41.135],[28.875,41.135],[28.875,41.060]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000031'; -- Gaziosmanpaşa

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.840,40.995],[28.910,40.995],[28.910,41.055],[28.840,41.055],[28.840,40.995]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000032'; -- Güngören

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.940,41.065],[29.035,41.065],[29.035,41.135],[28.940,41.135],[28.940,41.065]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000033'; -- Kağıthane

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.740,40.985],[28.870,40.985],[28.870,41.065],[28.740,41.065],[28.740,40.985]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000034'; -- Küçükçekmece

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.980,41.115],[29.100,41.115],[29.100,41.265],[28.980,41.265],[28.980,41.115]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000035'; -- Sarıyer

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[27.900,41.050],[28.300,41.050],[28.300,41.255],[27.900,41.255],[27.900,41.050]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000036'; -- Silivri

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.885,41.090],[28.975,41.090],[28.975,41.165],[28.885,41.165],[28.885,41.090]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000037'; -- Sultangazi

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.960,41.040],[29.055,41.040],[29.055,41.105],[28.960,41.105],[28.960,41.040]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000038'; -- Şişli

UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.870,40.985],[28.960,40.985],[28.960,41.040],[28.870,41.040],[28.870,40.985]]]}'::jsonb
WHERE id = '11111111-1111-1111-1111-000000000039'; -- Zeytinburnu
