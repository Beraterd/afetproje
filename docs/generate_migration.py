#!/usr/bin/env python3
"""
Istanbul assembly areas Excel -> Flyway SQL migration generator.
Output: backend/src/main/resources/db/migration/V40__istanbul_assembly_areas_import.sql
"""
import re, openpyxl, sys
from collections import defaultdict

# ===================================================================
# Normalization (mirrors Java AssemblyAreaExcelImportService.normalize)
# ===================================================================
def normalize(s):
    if not s: return ""
    s = s.lower()
    tr_map = {'ı':'i','ğ':'g','ü':'u','ş':'s','ö':'o','ç':'c','â':'a','î':'i','û':'u'}
    for k, v in tr_map.items():
        s = s.replace(k, v)
    s = re.sub(r'[^a-z0-9]', '', s)
    for suffix in ['mahallesi', 'mahalle', 'mah', 'mh']:
        if s.endswith(suffix) and len(s) > len(suffix):
            s = s[:-len(suffix)]
            break
    return s

def normalize_raw(s):
    """No suffix stripping - for compound word matching (e.g. Yenimahalle)."""
    if not s: return ""
    s = s.lower()
    tr_map = {'ı':'i','ğ':'g','ü':'u','ş':'s','ö':'o','ç':'c','â':'a','î':'i','û':'u'}
    for k, v in tr_map.items():
        s = s.replace(k, v)
    return re.sub(r'[^a-z0-9]', '', s)

def smart_match(excel_nh, db_nh_map):
    """Try suffix-stripped and raw-normalized matching."""
    for key in [normalize(excel_nh), normalize_raw(excel_nh)]:
        if key in db_nh_map:
            return db_nh_map[key]
    raw = normalize_raw(excel_nh)
    for db_key, db_name in db_nh_map.items():
        if raw == normalize_raw(db_name):
            return db_name
    return None

def sql_esc(s):
    if s is None: return "NULL"
    return "'" + s.replace("'", "''") + "'"

# ===================================================================
# District ID map (post-V6)
# ===================================================================
DISTRICT_IDS = {
    '11111111-1111-1111-1111-000000000001': 'Pendik',
    '11111111-1111-1111-1111-000000000002': 'Kartal',
    '11111111-1111-1111-1111-000000000003': 'Tuzla',
    '11111111-1111-1111-1111-000000000004': 'Kadıköy',
    '11111111-1111-1111-1111-000000000005': 'Ataşehir',
    '11111111-1111-1111-1111-000000000006': 'Bahçelievler',
    '11111111-1111-1111-1111-000000000007': 'Beşiktaş',
    '11111111-1111-1111-1111-000000000008': 'Bakırköy',
    '11111111-1111-1111-1111-000000000009': 'Fatih',
    '11111111-1111-1111-1111-000000000010': 'Beyoğlu',
    '11111111-1111-1111-1111-000000000011': 'Adalar',
    '11111111-1111-1111-1111-000000000012': 'Beykoz',
    '11111111-1111-1111-1111-000000000013': 'Çekmeköy',
    '11111111-1111-1111-1111-000000000014': 'Maltepe',
    '11111111-1111-1111-1111-000000000015': 'Sancaktepe',
    '11111111-1111-1111-1111-000000000016': 'Sultanbeyli',
    '11111111-1111-1111-1111-000000000017': 'Şile',
    '11111111-1111-1111-1111-000000000018': 'Ümraniye',
    '11111111-1111-1111-1111-000000000019': 'Üsküdar',
    '11111111-1111-1111-1111-000000000020': 'Arnavutköy',
    '11111111-1111-1111-1111-000000000021': 'Avcılar',
    '11111111-1111-1111-1111-000000000022': 'Bağcılar',
    '11111111-1111-1111-1111-000000000023': 'Başakşehir',
    '11111111-1111-1111-1111-000000000024': 'Bayrampaşa',
    '11111111-1111-1111-1111-000000000025': 'Beylikdüzü',
    '11111111-1111-1111-1111-000000000026': 'Büyükçekmece',
    '11111111-1111-1111-1111-000000000027': 'Çatalca',
    '11111111-1111-1111-1111-000000000028': 'Esenler',
    '11111111-1111-1111-1111-000000000029': 'Esenyurt',
    '11111111-1111-1111-1111-000000000030': 'Eyüpsultan',
    '11111111-1111-1111-1111-000000000031': 'Gaziosmanpaşa',
    '11111111-1111-1111-1111-000000000032': 'Güngören',
    '11111111-1111-1111-1111-000000000033': 'Kağıthane',
    '11111111-1111-1111-1111-000000000034': 'Küçükçekmece',
    '11111111-1111-1111-1111-000000000035': 'Sarıyer',
    '11111111-1111-1111-1111-000000000036': 'Silivri',
    '11111111-1111-1111-1111-000000000037': 'Sultangazi',
    '11111111-1111-1111-1111-000000000038': 'Şişli',
    '11111111-1111-1111-1111-000000000039': 'Zeytinburnu',
}
DIST_NAME_TO_ID = {v: k for k, v in DISTRICT_IDS.items()}

DISTRICT_NORM = {normalize(v): v for v in set(DISTRICT_IDS.values())}
DISTRICT_NORM['kcekmece'] = 'Küçükçekmece'
DISTRICT_NORM['kucukcekmece'] = 'Küçükçekmece'

# ===================================================================
# Explicit aliases: (db_district_name, excel_nh_raw) -> db_nh_name | None
# None = skip (ambiguous or wrong), '__NEW__' = add as new neighborhood
# ===================================================================
EXPLICIT = {
    ('Bayrampaşa',   'Ortamahalle Mh.'): 'Orta',
    ('Beykoz',       'Cumhuriyet Mh.'): 'Cumhuriyetköy',
    ('Beykoz',       'Çiftlik Mh.'): None,
    ('Büyükçekmece', 'Mimar Sinan Merkez Mh.'): 'Mimarsinan',
    ('Büyükçekmece', '20 Mayıs Mh.'): '__NEW__',
    ('Esenyurt',     'Akevler Mh.'): 'Akevler Mahallesi 7 sokak',
    ('Esenyurt',     'Bağlarçeşme Mh.'): '__NEW__',
    ('Esenler',      'Havaalanı Mh.'): '__NEW__',
    ('Güngören',     'Yıldıztabya Mh.'): '__NEW__',
    ('Sarıyer',      'Kumköy Mh.'): 'Kumköy (Kilyos)',
    ('Silivri',      'Balaban Mh.'): 'Çanta Balaban',
    ('Silivri',      'Fevzipaşa Mh.'): 'Değirmenköy Fevzipaşa',
    ('Silivri',      'Hürriyet Mh.'): 'Kavaklar Hürriyet',
    ('Silivri',      'Kavaklar Mh.'): None,
    ('Silivri',      'Sancaktepe Mh.'): 'Çanta Sancaktepe',
    ('Silivri',      'İsmetpaşa Mh.'): 'Değirmenköy İsmetpaşa',
    ('Tuzla',        'Camii Mah.'): 'Cami',
    ('Çatalca',      'Belgrad Mh.'): 'Belgrat',
    ('Ümraniye',     'Saray Mh.'): '__NEW__',
    ('Şile',         'Kalemköy Mh.'): 'Kalem',
    ('Şile',         'Koruköy Mh.'): 'Korucu',
}

# New neighborhood canonical names indexed by (district, excel_nh_raw)
NEW_NEIGHBORHOOD_MAP = {
    ('Büyükçekmece', '20 Mayıs Mh.'): '20 Mayıs',
    ('Esenyurt',     'Bağlarçeşme Mh.'): 'Bağlarçeşme',
    ('Esenler',      'Havaalanı Mh.'): 'Havaalanı',
    ('Güngören',     'Yıldıztabya Mh.'): 'Yıldıztabya',
    ('Ümraniye',     'Saray Mh.'): 'Saray',
}

# Kağıthane - 12 neighborhoods missing from DB
KAGITHANE_NEW = [
    'Emniyet Evleri', 'Gürsel', 'Harmantepe', 'Hürriyet',
    'Mehmet Akif Ersoy', 'Ortabayır', 'Sultan Selim', 'Talatpaşa',
    'Telsizler', 'Yeşilce', 'Çağlayan', 'Şirintepe',
]

# ===================================================================
# Load DB neighborhoods from migrations
# ===================================================================
def extract_neighborhoods_from_sql(filepath):
    result = []
    try:
        with open(filepath, encoding='utf-8', errors='replace') as f:
            content = f.read()
    except FileNotFoundError:
        return result
    pattern = r"\((?:gen_random_uuid\(\)|'[0-9a-f-]+')\s*,\s*'([^']+)'\s*,\s*'([0-9a-f-]+)'"
    for name, dist_id in re.findall(pattern, content, re.IGNORECASE):
        dist_name = DISTRICT_IDS.get(dist_id)
        if dist_name:
            result.append((name, dist_name))
    return result

db_nh_by_district = defaultdict(dict)  # dist_name -> {norm_key: real_name}

for sql_file in [
    'backend/src/main/resources/db/migration/V5__seed_data.sql',
    'backend/src/main/resources/db/migration/V6__update_pilot_districts_and_neighborhoods.sql',
    'backend/src/main/resources/db/migration/V18__complete_neighborhood_boundaries.sql',
    'backend/src/main/resources/db/migration/V24__add_remaining_29_districts.sql',
    'backend/src/main/resources/db/migration/V27__osm_all39_boundaries.sql',
]:
    for name, dist in extract_neighborhoods_from_sql(sql_file):
        db_nh_by_district[dist][normalize(name)] = name
        db_nh_by_district[dist][normalize_raw(name)] = name

# Register new neighborhoods in lookup maps
all_new_neighborhoods = []  # (district_name, neighborhood_name)

for nh in KAGITHANE_NEW:
    all_new_neighborhoods.append(('Kağıthane', nh))
    db_nh_by_district['Kağıthane'][normalize(nh)] = nh
    db_nh_by_district['Kağıthane'][normalize_raw(nh)] = nh

for (dist, _), nh_name in NEW_NEIGHBORHOOD_MAP.items():
    if (dist, nh_name) not in all_new_neighborhoods:
        all_new_neighborhoods.append((dist, nh_name))
    db_nh_by_district[dist][normalize(nh_name)] = nh_name
    db_nh_by_district[dist][normalize_raw(nh_name)] = nh_name

# ===================================================================
# Read Excel and map
# ===================================================================
wb = openpyxl.load_workbook("docs/istanbul_toplanma_master.xlsx", read_only=True, data_only=True)
ws = wb.active
excel_rows = list(ws.iter_rows(min_row=2, values_only=True))
wb.close()

seen_areas = set()   # (dist_norm, nh_norm, area_norm) dedup
insert_rows = []     # (db_dist_name, db_nh_name, area_name, capacity, url)
skipped = []
skipped_dup = 0

for row in excel_rows:
    if not row or all(v is None for v in row):
        continue
    excel_dist = (str(row[0]) if row[0] else "").strip()
    excel_nh   = (str(row[1]) if row[1] else "").strip()
    area_name  = (str(row[2]) if row[2] else "").strip()
    area_m2    = row[3]
    url        = (str(row[4]) if row[4] else "").strip()

    if not excel_dist or not area_name:
        skipped.append(f"EMPTY: '{excel_dist}' / '{excel_nh}' / '{area_name}'")
        continue

    norm_dist = normalize(excel_dist)
    if norm_dist == 'kcekmece':
        norm_dist = 'kucukcekmece'
    db_dist = DISTRICT_NORM.get(norm_dist)
    if not db_dist:
        skipped.append(f"NO_DIST: '{excel_dist}'")
        continue

    explicit_key = (db_dist, excel_nh)
    if explicit_key in EXPLICIT:
        alias = EXPLICIT[explicit_key]
        if alias is None:
            skipped.append(f"SKIP_AMBIGUOUS: {db_dist} / {excel_nh}")
            continue
        if alias == '__NEW__':
            db_nh = NEW_NEIGHBORHOOD_MAP[explicit_key]
        else:
            db_nh = alias
    else:
        db_nh = smart_match(excel_nh, db_nh_by_district.get(db_dist, {}))
        if not db_nh:
            skipped.append(f"NO_NH: {db_dist} / {excel_nh}")
            continue

    dedup_key = (normalize(db_dist), normalize(db_nh), normalize(area_name))
    if dedup_key in seen_areas:
        skipped_dup += 1
        continue
    seen_areas.add(dedup_key)

    capacity = None
    if area_m2 is not None:
        try:
            capacity = int(float(str(area_m2)))
        except (ValueError, TypeError):
            pass

    insert_rows.append((db_dist, db_nh, area_name, capacity, url or None))

# ===================================================================
# Generate SQL migration
# ===================================================================
lines = []
lines.append("-- =====================================================================")
lines.append("-- V40: İstanbul geneli toplanma alanları import")
lines.append("--")
lines.append("-- Kaynak: docs/istanbul_toplanma_master.xlsx")
lines.append("-- Toplam kayıt: " + str(len(insert_rows)))
lines.append("-- Yeni mahalle eklendi: " + str(len(all_new_neighborhoods)))
lines.append("-- Atlandı (duplicate/belirsiz): " + str(skipped_dup + len(skipped)))
lines.append("-- source_name: 'İstanbul Toplanma Alanları Master (Excel)'")
lines.append("-- needs_review: FALSE (Google Maps URL mevcut)")
lines.append("-- is_active: TRUE")
lines.append("-- =====================================================================")
lines.append("")

# Section 1: New neighborhoods
if all_new_neighborhoods:
    lines.append("-- =====================================================================")
    lines.append("-- BÖLÜM 1: Eksik mahalleler (migration'larda bulunmayan)")
    lines.append("-- =====================================================================")
    lines.append("")

    by_dist = defaultdict(list)
    for dist, nh in all_new_neighborhoods:
        by_dist[dist].append(nh)

    for dist in sorted(by_dist.keys()):
        dist_id = DIST_NAME_TO_ID[dist]
        lines.append(f"-- {dist}")
        lines.append("INSERT INTO neighborhoods (id, name, district_id, created_at, updated_at)")
        lines.append("VALUES")
        nhs = by_dist[dist]
        for i, nh in enumerate(nhs):
            comma = "," if i < len(nhs) - 1 else ""
            lines.append(f"    (gen_random_uuid(), {sql_esc(nh)}, '{dist_id}', now(), now()){comma}")
        lines.append("ON CONFLICT (name, district_id) DO NOTHING;")
        lines.append("")

# Section 2: Assembly areas
lines.append("-- =====================================================================")
lines.append("-- BÖLÜM 2: Toplanma alanları")
lines.append("-- =====================================================================")
lines.append("")
lines.append("INSERT INTO assembly_areas")
lines.append("    (id, neighborhood_id, district_id, name, capacity, google_maps_url,")
lines.append("     source_name, needs_review, is_active, created_at)")
lines.append("SELECT")
lines.append("    gen_random_uuid(),")
lines.append("    n.id,")
lines.append("    d.id,")
lines.append("    aa.name,")
lines.append("    aa.capacity,")
lines.append("    aa.google_maps_url,")
lines.append("    'İstanbul Toplanma Alanları Master (Excel)',")
lines.append("    FALSE,")
lines.append("    TRUE,")
lines.append("    now()")
lines.append("FROM (VALUES")

for i, (db_dist, db_nh, area_name, capacity, url) in enumerate(insert_rows):
    cap_sql = str(capacity) if capacity is not None else "NULL"
    comma = "," if i < len(insert_rows) - 1 else ""
    lines.append(f"    ({sql_esc(db_dist)}, {sql_esc(db_nh)}, {sql_esc(area_name)}, {cap_sql}::int, {sql_esc(url)}){comma}")

lines.append(") AS aa(district_name, neighborhood_name, name, capacity, google_maps_url)")
lines.append("JOIN districts d ON d.name = aa.district_name")
lines.append("JOIN neighborhoods n ON n.name = aa.neighborhood_name AND n.district_id = d.id")
lines.append("-- Duplicate koruma: ayni mahalle + ayni ad zaten varsa insert etme")
lines.append("WHERE NOT EXISTS (")
lines.append("    SELECT 1 FROM assembly_areas ex")
lines.append("    WHERE ex.neighborhood_id = n.id")
lines.append("      AND LOWER(TRIM(ex.name)) = LOWER(TRIM(aa.name))")
lines.append(");")
lines.append("")

out_path = "backend/src/main/resources/db/migration/V40__istanbul_assembly_areas_import.sql"
with open(out_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print("=== OZET ===")
print(f"Toplam Excel satiri: {len(excel_rows)}")
print(f"Insert edilecek: {len(insert_rows)}")
print(f"Duplicate atlandi: {skipped_dup}")
print(f"Atlanan (diger): {len(skipped)}")
print(f"Yeni mahalle: {len(all_new_neighborhoods)}")
print(f"SQL migration: {out_path}")
print(f"Dosya boyutu: {len(chr(10).join(lines))} karakter")

skip_summary = defaultdict(int)
for s in skipped:
    t = s.split(':')[0]
    skip_summary[t] += 1
print(f"\nAtlama ozeti:")
for t, cnt in sorted(skip_summary.items()):
    print(f"  {t}: {cnt}")

if [s for s in skipped if s.startswith('NO_NH')]:
    print("\nEslesmeyen mahalleler (NO_NH):")
    for s in sorted(s for s in skipped if s.startswith('NO_NH')):
        print(f"  {s}")
