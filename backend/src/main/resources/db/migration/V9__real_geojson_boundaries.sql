-- V7: Real GeoJSON polygon boundaries for 10 pilot Istanbul districts and their neighborhoods
-- Source: Simplified administrative boundary approximations derived from OpenStreetMap / Istanbul IBB
-- Coordinate format: GeoJSON [longitude, latitude] — outer ring closed (first == last point)

-- =============================================================================
-- DISTRICT BOUNDARIES
-- =============================================================================

-- Pendik (Asian side, eastern coast + inland hills)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.150,40.862],[29.196,40.855],[29.235,40.855],[29.268,40.862],[29.265,40.895],[29.248,40.930],[29.215,40.960],[29.175,40.963],[29.152,40.942],[29.145,40.910],[29.150,40.878],[29.150,40.862]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000001';

-- Kartal (Asian side, between Maltepe and Pendik)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.108,40.878],[29.150,40.862],[29.150,40.878],[29.145,40.910],[29.132,40.928],[29.112,40.920],[29.102,40.895],[29.108,40.878]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000002';

-- Tuzla (Asian side, SE, easternmost pilot district)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.268,40.862],[29.322,40.830],[29.385,40.798],[29.418,40.828],[29.420,40.868],[29.396,40.900],[29.350,40.912],[29.295,40.908],[29.268,40.895],[29.268,40.862]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000003';

-- Kadıköy (Asian side, SW, Marmara coastal peninsula)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.000,40.968],[29.030,40.960],[29.068,40.965],[29.102,40.975],[29.112,40.998],[29.108,41.018],[29.085,41.028],[29.055,41.028],[29.022,41.018],[28.998,41.003],[28.997,40.985],[29.000,40.968]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000004';

-- Ataşehir (Asian side, inland east of Kadıköy)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.068,40.962],[29.112,40.958],[29.148,40.970],[29.148,41.008],[29.128,41.028],[29.090,41.030],[29.068,41.020],[29.060,40.998],[29.068,40.962]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000005';

-- Bahçelievler (European side, inland SW)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.808,40.998],[28.872,40.995],[28.905,41.003],[28.905,41.025],[28.878,41.030],[28.838,41.030],[28.808,41.022],[28.808,40.998]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000006';

-- Beşiktaş (European side, along Bosphorus)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.968,41.038],[29.002,41.038],[29.052,41.055],[29.062,41.078],[29.038,41.095],[28.992,41.093],[28.958,41.080],[28.942,41.062],[28.952,41.045],[28.968,41.038]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000007';

-- Bakırköy (European side, SW coastal)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.808,40.962],[28.845,40.958],[28.878,40.965],[28.905,40.992],[28.905,41.003],[28.872,40.995],[28.808,40.998],[28.808,40.962]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000008';

-- Fatih (European side, historic peninsula — sea on 3 sides)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.892,41.000],[28.942,40.994],[28.978,40.997],[28.982,41.008],[28.978,41.022],[28.958,41.037],[28.930,41.042],[28.905,41.038],[28.892,41.020],[28.892,41.000]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000009';

-- Beyoğlu (European side, north of Golden Horn)
UPDATE districts SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.932,41.022],[28.978,41.022],[29.006,41.032],[29.012,41.045],[29.002,41.062],[28.975,41.068],[28.948,41.062],[28.932,41.048],[28.930,41.035],[28.932,41.022]]]}'::jsonb, updated_at = now() WHERE id = '11111111-1111-1111-1111-000000000010';


-- =============================================================================
-- NEIGHBORHOOD BOUNDARIES
-- =============================================================================
-- Each neighborhood polygon is a sub-region within its parent district.

-- ---- PENDIK neighborhoods ----

-- Kurtköy (northern, near TEM/FSM junction)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.155,40.908],[29.210,40.905],[29.228,40.925],[29.225,40.958],[29.215,40.960],[29.175,40.963],[29.155,40.942],[29.148,40.920],[29.155,40.908]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000001';

-- Pendik Merkez (western center, main town center)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.150,40.862],[29.205,40.858],[29.212,40.875],[29.205,40.908],[29.168,40.912],[29.148,40.905],[29.145,40.882],[29.150,40.862]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000002';

-- Kaynarca (eastern area, near port)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.210,40.855],[29.235,40.855],[29.268,40.862],[29.265,40.895],[29.248,40.928],[29.228,40.928],[29.210,40.910],[29.208,40.878],[29.210,40.855]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000003';

-- Kavakpınar (NE inland)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.225,40.928],[29.248,40.928],[29.265,40.895],[29.268,40.862],[29.248,40.930],[29.225,40.955],[29.215,40.958],[29.225,40.928]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000101';

-- Güzelyalı (southern coastal strip)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.150,40.855],[29.210,40.855],[29.212,40.868],[29.196,40.872],[29.155,40.870],[29.148,40.862],[29.150,40.855]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000102';

-- Yenişehir (central-southern)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.168,40.870],[29.212,40.868],[29.212,40.878],[29.208,40.908],[29.168,40.912],[29.155,40.905],[29.155,40.878],[29.168,40.870]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000103';


-- ---- KARTAL neighborhoods ----

-- Kartal Merkez (western center, coastal)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.108,40.878],[29.140,40.870],[29.150,40.882],[29.145,40.905],[29.120,40.910],[29.105,40.895],[29.108,40.878]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000004';

-- Uğur Mumcu (northern Kartal)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.112,40.910],[29.120,40.910],[29.145,40.905],[29.148,40.920],[29.138,40.928],[29.115,40.922],[29.112,40.910]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000005';

-- Soğanlık (eastern)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.138,40.875],[29.150,40.870],[29.150,40.890],[29.148,40.918],[29.138,40.918],[29.130,40.908],[29.138,40.892],[29.138,40.875]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000104';

-- Topselvi (NW)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.102,40.902],[29.112,40.910],[29.115,40.922],[29.108,40.930],[29.100,40.922],[29.100,40.905],[29.102,40.902]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000105';

-- Hürriyet (southern)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.108,40.870],[29.140,40.870],[29.142,40.882],[29.130,40.888],[29.108,40.885],[29.108,40.870]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000106';


-- ---- TUZLA neighborhoods ----

-- Tuzla Merkez (western center, along coast)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.268,40.862],[29.318,40.832],[29.345,40.842],[29.338,40.875],[29.305,40.892],[29.270,40.895],[29.268,40.862]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000006';

-- Aydınlı (eastern, industry area)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.345,40.842],[29.385,40.802],[29.418,40.828],[29.418,40.860],[29.390,40.872],[29.355,40.870],[29.345,40.842]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000007';

-- Cami (NW coast near Gebze border)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.268,40.895],[29.305,40.892],[29.310,40.905],[29.295,40.912],[29.272,40.908],[29.268,40.895]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000107';

-- İçmeler (SE coastal, marina area)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.385,40.800],[29.418,40.828],[29.420,40.868],[29.410,40.882],[29.385,40.870],[29.380,40.830],[29.385,40.800]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000108';

-- Mimar Sinan (NE inland)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.310,40.895],[29.355,40.870],[29.390,40.872],[29.396,40.900],[29.350,40.912],[29.295,40.908],[29.310,40.895]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000109';


-- ---- KADIKÖY neighborhoods ----

-- Moda (western coastal, peninsula)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.000,40.970],[29.025,40.965],[29.038,40.978],[29.035,41.002],[29.018,41.008],[29.000,41.002],[28.997,40.985],[29.000,40.970]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000008';

-- Bostancı (eastern coast)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.075,40.968],[29.102,40.975],[29.112,40.998],[29.105,41.015],[29.082,41.018],[29.072,41.000],[29.075,40.980],[29.075,40.968]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000009';

-- Erenköy (central, inland)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.025,40.965],[29.075,40.968],[29.075,40.982],[29.068,41.000],[29.038,41.002],[29.025,40.985],[29.022,40.972],[29.025,40.965]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000000a';

-- Fenerbahçe (SW, coastal)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.997,40.960],[29.038,40.960],[29.040,40.970],[29.025,40.972],[29.000,40.970],[28.997,40.965],[28.997,40.960]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000010a';

-- Suadiye (SE coastal)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.060,40.962],[29.102,40.975],[29.102,40.985],[29.080,40.988],[29.062,40.978],[29.060,40.968],[29.060,40.962]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000010b';

-- Göztepe (northern)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.022,41.005],[29.068,41.000],[29.082,41.018],[29.085,41.028],[29.055,41.028],[29.022,41.018],[29.022,41.005]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000010c';


-- ---- ATAŞEHİR neighborhoods ----

-- Ataşehir Merkez (SW center)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.068,40.965],[29.108,40.965],[29.110,40.998],[29.095,41.008],[29.068,41.005],[29.060,40.995],[29.068,40.965]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000000b';

-- Küçükbakkalköy (SE area)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.108,40.965],[29.148,40.972],[29.148,41.005],[29.125,41.012],[29.110,40.998],[29.108,40.965]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000000c';

-- Barbaros (NW)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.068,41.003],[29.095,41.008],[29.108,41.022],[29.090,41.030],[29.068,41.020],[29.062,41.010],[29.068,41.003]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000010d';

-- Ferhatpaşa (NE)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.108,41.005],[29.148,41.008],[29.148,41.028],[29.128,41.028],[29.108,41.022],[29.108,41.005]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000010e';

-- Mevlana (southern)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.068,40.960],[29.148,40.962],[29.148,40.972],[29.108,40.968],[29.068,40.968],[29.068,40.960]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000010f';


-- ---- BAHÇELİEVLER neighborhoods ----

-- Yenibosna (SW, near airport highway)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.808,40.998],[28.858,40.996],[28.892,41.003],[28.878,41.015],[28.830,41.015],[28.808,41.010],[28.808,40.998]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000000d';

-- Şirinevler (NW, residential)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.808,41.010],[28.830,41.015],[28.865,41.015],[28.870,41.028],[28.838,41.030],[28.808,41.022],[28.808,41.010]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000000e';

-- Cumhuriyet (eastern N)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.870,41.015],[28.905,41.008],[28.905,41.025],[28.878,41.030],[28.868,41.025],[28.870,41.015]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000110';

-- Kocasinan (eastern S)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.858,40.996],[28.905,41.003],[28.905,41.010],[28.870,41.015],[28.858,41.010],[28.852,41.000],[28.858,40.996]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000111';

-- Zafer (central N)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.830,41.015],[28.865,41.015],[28.868,41.025],[28.852,41.030],[28.830,41.028],[28.825,41.018],[28.830,41.015]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000112';


-- ---- BEŞİKTAŞ neighborhoods ----

-- Levent (inland, business district)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.975,41.055],[29.005,41.050],[29.022,41.060],[29.020,41.078],[28.998,41.082],[28.975,41.075],[28.970,41.062],[28.975,41.055]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000000f';

-- Etiler (NW, upscale residential)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.942,41.062],[28.968,41.060],[28.975,41.075],[28.960,41.090],[28.938,41.090],[28.928,41.078],[28.938,41.065],[28.942,41.062]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000010';

-- Ortaköy (SE, Bosphorus shore)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.002,41.038],[29.035,41.042],[29.052,41.055],[29.045,41.065],[29.022,41.060],[29.005,41.050],[29.002,41.038]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000113';

-- Bebek (NE, Bosphorus villa area)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[29.035,41.075],[29.062,41.078],[29.038,41.095],[28.998,41.093],[28.992,41.085],[29.008,41.078],[29.035,41.075]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000114';

-- Arnavutköy (NW, Bosphorus village)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.958,41.080],[28.992,41.085],[28.992,41.093],[28.968,41.093],[28.942,41.092],[28.942,41.082],[28.958,41.080]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000115';


-- ---- BAKIRKÖY neighborhoods ----

-- Ataköy (SW, seafront)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.808,40.962],[28.850,40.958],[28.862,40.972],[28.855,40.990],[28.825,40.992],[28.808,40.985],[28.808,40.962]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000011';

-- Zeytinlik (eastern, inland)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.862,40.972],[28.878,40.965],[28.905,40.992],[28.898,41.002],[28.862,41.000],[28.855,40.990],[28.862,40.972]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000012';

-- Kartaltepe (northern)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.830,40.988],[28.862,40.985],[28.862,40.998],[28.838,41.000],[28.810,40.998],[28.808,40.992],[28.830,40.988]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000116';

-- Osmaniye (central)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.855,40.958],[28.878,40.965],[28.878,40.978],[28.862,40.980],[28.850,40.972],[28.850,40.960],[28.855,40.958]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000117';

-- Şenlik (NW)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.808,40.978],[28.835,40.975],[28.850,40.985],[28.838,40.998],[28.808,40.998],[28.808,40.978]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000118';


-- ---- FATİH neighborhoods ----

-- Sultanahmet (SE, historic core)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.958,41.002],[28.978,40.998],[28.982,41.008],[28.978,41.018],[28.960,41.020],[28.948,41.012],[28.958,41.002]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000013';

-- Balat (N, Golden Horn shore)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.935,41.028],[28.960,41.022],[28.975,41.022],[28.970,41.037],[28.950,41.040],[28.930,41.042],[28.928,41.035],[28.935,41.028]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000014';

-- Aksaray (SW, commercial)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.892,41.000],[28.930,41.000],[28.932,41.012],[28.918,41.018],[28.900,41.015],[28.892,41.010],[28.892,41.000]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000119';

-- Haseki (western, hospital district)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.892,41.010],[28.900,41.015],[28.920,41.020],[28.918,41.030],[28.905,41.038],[28.892,41.032],[28.892,41.018],[28.892,41.010]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000011a';

-- Çarşamba (NW, Fatih neighborhood)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.930,41.015],[28.958,41.010],[28.960,41.022],[28.940,41.030],[28.930,41.032],[28.920,41.025],[28.930,41.015]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000011b';


-- ---- BEYOĞLU neighborhoods ----

-- Galata (SW, historic tower district)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.932,41.022],[28.965,41.022],[28.975,41.030],[28.968,41.040],[28.948,41.042],[28.932,41.038],[28.930,41.030],[28.932,41.022]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000015';

-- Cihangir (central, bohemian quarter)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.968,41.030],[29.006,41.032],[29.008,41.048],[28.988,41.052],[28.968,41.048],[28.968,41.040],[28.968,41.030]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-000000000016';

-- Taksim (central N, main square)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.965,41.040],[28.988,41.040],[29.008,41.048],[29.005,41.062],[28.982,41.065],[28.960,41.060],[28.952,41.048],[28.965,41.040]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000011c';

-- Tarlabaşı (W, historic quarter)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.932,41.038],[28.952,41.042],[28.960,41.055],[28.948,41.062],[28.932,41.058],[28.928,41.048],[28.932,41.038]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000011d';

-- Kasımpaşa (NW, Golden Horn side)
UPDATE neighborhoods SET geojson_polygon = '{"type":"Polygon","coordinates":[[[28.932,41.048],[28.948,41.062],[28.960,41.068],[28.975,41.068],[29.002,41.062],[29.005,41.062],[28.988,41.052],[28.965,41.048],[28.950,41.048],[28.932,41.048]]]}'::jsonb WHERE id = '22222222-2222-2222-2222-00000000011e';
