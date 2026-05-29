#!/usr/bin/env python3
"""
fetch_istanbul_boundaries.py

Fetches real Istanbul district and neighborhood administrative boundaries
from OpenStreetMap Overpass API, validates them with Shapely, and writes
a Flyway SQL migration.

DATA SOURCE:
    OpenStreetMap contributors — https://www.openstreetmap.org
    License: ODbL  https://opendatacommons.org/licenses/odbl/

VALIDATION:
    - Shapely make_valid() repairs invalid rings
    - Neighborhood polygons clipped to district polygon
      (guarantees containment, removes sea / cross-district bleed)

REQUIREMENTS:
    pip install requests shapely

USAGE:
    python scripts/fetch_istanbul_boundaries.py

    # Output: backend/src/main/resources/db/migration/V27__osm_all39_boundaries.sql
    # Apply with Flyway before starting the backend.
    #
    # To run only specific districts (faster testing):
    # DISTRICTS_FILTER="Şişli,Beykoz" python scripts/fetch_istanbul_boundaries.py
"""

import json, os, sys, time
from typing import Optional
import requests
from shapely.geometry import shape, mapping, Polygon, MultiPolygon, GeometryCollection
from shapely.ops import unary_union
from shapely.validation import make_valid

# ---------------------------------------------------------------------------
OVERPASS_ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass.openstreetmap.ru/api/interpreter",
]
REQUEST_DELAY = 5   # seconds between requests (be polite to Overpass)
RETRY_DELAY   = 30  # seconds to wait after rate-limit / timeout
SIMPLIFY_TOL  = 0.00005  # Douglas-Peucker tolerance (~5m)
MIN_AREA      = 5e-8     # minimum polygon area to keep

# ---------------------------------------------------------------------------
# All 39 Istanbul districts — name must match OSM "name" tag exactly.
# UUID format: 11111111-1111-1111-1111-0000000000XX
#   IDs 01-10 : original 10 pilot districts (V5/V6)
#   IDs 11-39 : new districts added in V24
# ---------------------------------------------------------------------------
DISTRICTS: dict[str, str] = {
    # ── Original 10 pilot districts ──────────────────────────────────────────
    "Pendik":        "11111111-1111-1111-1111-000000000001",
    "Kartal":        "11111111-1111-1111-1111-000000000002",
    "Tuzla":         "11111111-1111-1111-1111-000000000003",
    "Kadıköy":       "11111111-1111-1111-1111-000000000004",
    "Ataşehir":      "11111111-1111-1111-1111-000000000005",
    "Bahçelievler":  "11111111-1111-1111-1111-000000000006",
    "Beşiktaş":      "11111111-1111-1111-1111-000000000007",
    "Bakırköy":      "11111111-1111-1111-1111-000000000008",
    "Fatih":         "11111111-1111-1111-1111-000000000009",
    "Beyoğlu":       "11111111-1111-1111-1111-000000000010",
    # ── 29 new districts (V24) ───────────────────────────────────────────────
    # Anadolu yakası
    "Adalar":        "11111111-1111-1111-1111-000000000011",
    "Beykoz":        "11111111-1111-1111-1111-000000000012",
    "Çekmeköy":      "11111111-1111-1111-1111-000000000013",
    "Maltepe":       "11111111-1111-1111-1111-000000000014",
    "Sancaktepe":    "11111111-1111-1111-1111-000000000015",
    "Sultanbeyli":   "11111111-1111-1111-1111-000000000016",
    "Şile":          "11111111-1111-1111-1111-000000000017",
    "Ümraniye":      "11111111-1111-1111-1111-000000000018",
    "Üsküdar":       "11111111-1111-1111-1111-000000000019",
    # Avrupa yakası
    "Arnavutköy":    "11111111-1111-1111-1111-000000000020",
    "Avcılar":       "11111111-1111-1111-1111-000000000021",
    "Bağcılar":      "11111111-1111-1111-1111-000000000022",
    "Başakşehir":    "11111111-1111-1111-1111-000000000023",
    "Bayrampaşa":    "11111111-1111-1111-1111-000000000024",
    "Beylikdüzü":    "11111111-1111-1111-1111-000000000025",
    "Büyükçekmece":  "11111111-1111-1111-1111-000000000026",
    "Çatalca":       "11111111-1111-1111-1111-000000000027",
    "Esenler":       "11111111-1111-1111-1111-000000000028",
    "Esenyurt":      "11111111-1111-1111-1111-000000000029",
    "Eyüpsultan":    "11111111-1111-1111-1111-000000000030",
    "Gaziosmanpaşa": "11111111-1111-1111-1111-000000000031",
    "Güngören":      "11111111-1111-1111-1111-000000000032",
    "Kağıthane":     "11111111-1111-1111-1111-000000000033",
    "Küçükçekmece":  "11111111-1111-1111-1111-000000000034",
    "Sarıyer":       "11111111-1111-1111-1111-000000000035",
    "Silivri":       "11111111-1111-1111-1111-000000000036",
    "Sultangazi":    "11111111-1111-1111-1111-000000000037",
    "Şişli":         "11111111-1111-1111-1111-000000000038",
    "Zeytinburnu":   "11111111-1111-1111-1111-000000000039",
}

# Expanded bbox covering ALL 39 districts (incl. Silivri west, Şile east, Çatalca north)
# Format: south,west,north,east
ISTANBUL_BBOX = "40.70,27.75,41.70,30.00"

SCRIPT_DIR   = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUT_SQL      = os.path.join(
    PROJECT_ROOT, "backend", "src", "main", "resources",
    "db", "migration", "V27__osm_all39_boundaries.sql"
)

# Optional env-var filter for testing: DISTRICTS_FILTER="Şişli,Beykoz"
_FILTER = os.environ.get("DISTRICTS_FILTER", "").strip()
if _FILTER:
    FILTER_SET = {n.strip() for n in _FILTER.split(",")}
    DISTRICTS  = {k: v for k, v in DISTRICTS.items() if k in FILTER_SET}

# ---------------------------------------------------------------------------
_ep_idx = 0
def log(m): print(m, file=sys.stderr, flush=True)

def overpass(query: str) -> dict:
    global _ep_idx
    for attempt in range(len(OVERPASS_ENDPOINTS) * 2):
        url = OVERPASS_ENDPOINTS[_ep_idx % len(OVERPASS_ENDPOINTS)]
        try:
            log(f"    [{url.split('/')[2][:20]}] attempt {attempt+1}")
            resp = requests.post(url, data={"data": query}, timeout=300,
                                 headers={"Accept-Charset": "utf-8"})
            if resp.status_code in (429, 503, 504):
                log(f"    HTTP {resp.status_code} — rotate + sleep {RETRY_DELAY}s")
                _ep_idx += 1; time.sleep(RETRY_DELAY); continue
            resp.raise_for_status()
            if not resp.text.strip():
                log("    Empty response body — rotate + sleep")
                _ep_idx += 1; time.sleep(RETRY_DELAY); continue
            return resp.json()
        except requests.Timeout:
            log(f"    Timeout — rotate + sleep {RETRY_DELAY}s")
            _ep_idx += 1; time.sleep(RETRY_DELAY)
        except Exception as e:
            log(f"    Error: {e}")
            _ep_idx += 1; time.sleep(RETRY_DELAY)
    raise RuntimeError("All Overpass endpoints exhausted")

# ---------------------------------------------------------------------------
def _eq(a, b, tol=1e-7): return abs(a[0]-b[0])<tol and abs(a[1]-b[1])<tol

def assemble_rings(way_members: list) -> list:
    segs = []
    for m in way_members:
        pts = m.get("geometry", [])
        if len(pts) >= 2:
            segs.append([(p["lon"], p["lat"]) for p in pts])
    if not segs: return []
    used = [False]*len(segs)
    rings = []
    for start in range(len(segs)):
        if used[start]: continue
        ring = list(segs[start]); used[start] = True
        changed = True
        while changed:
            changed = False
            for i, seg in enumerate(segs):
                if used[i]: continue
                tail, head = ring[-1], ring[0]
                if   _eq(tail, seg[0]):  ring.extend(seg[1:]); used[i]=True; changed=True
                elif _eq(tail, seg[-1]): ring.extend(list(reversed(seg))[1:]); used[i]=True; changed=True
                elif _eq(head, seg[-1]): ring=list(seg)+ring[1:]; used[i]=True; changed=True
                elif _eq(head, seg[0]):  ring=list(reversed(seg))+ring[1:]; used[i]=True; changed=True
        if not _eq(ring[0], ring[-1]): ring.append(ring[0])
        if len(ring) >= 4: rings.append(ring)
    return rings

def rel_to_geom(members: list) -> Optional[dict]:
    outers = [m for m in members if m.get("role")=="outer" and "geometry" in m]
    inners = [m for m in members if m.get("role")=="inner" and "geometry" in m]
    outer_rings = assemble_rings(outers)
    inner_rings  = assemble_rings(inners)
    if not outer_rings: return None
    if len(outer_rings) == 1:
        return {"type":"Polygon","coordinates":[outer_rings[0]]+inner_rings}
    outer_shapes = [Polygon(r) for r in outer_rings]
    polys = []
    for idx, outer in enumerate(outer_rings):
        inners_assigned = [ir for ir in inner_rings
                           if outer_shapes[idx].contains(Polygon(ir).centroid)]
        polys.append([outer]+inners_assigned)
    return {"type":"MultiPolygon","coordinates":polys}

def extract_polygons(geom):
    if isinstance(geom, (Polygon, MultiPolygon)): return geom
    if isinstance(geom, GeometryCollection):
        parts = [g for g in geom.geoms if isinstance(g,(Polygon,MultiPolygon))]
        return unary_union(parts) if parts else None
    return None

def validate(raw: dict, label: str) -> Optional[dict]:
    try:   geom = shape(raw)
    except Exception as e: log(f"    [SKIP] parse error '{label}': {e}"); return None
    if not geom.is_valid: geom = make_valid(geom)
    geom = extract_polygons(geom)
    if geom is None or geom.is_empty or geom.area < MIN_AREA: return None
    geom = geom.simplify(SIMPLIFY_TOL, preserve_topology=True)
    return json.loads(json.dumps(mapping(geom)))

def norm(name: str) -> str:
    name = name.strip()
    for sfx in [" Mahallesi"," Mah."," Mh."," mahallesi"]:
        if name.lower().endswith(sfx.lower()):
            name=name[:-len(sfx)].strip(); break
    return name

# ---------------------------------------------------------------------------
def fetch_district(name: str) -> Optional[tuple]:
    """Return (geojson, osm_relation_id) for one Istanbul district."""
    q = (f'[out:json][timeout:90][bbox:{ISTANBUL_BBOX}];\n'
         f'rel["name"="{name}"]["admin_level"="6"]["boundary"="administrative"];\n'
         'out geom;')
    log(f"  District: {name}")
    data = overpass(q)
    rels = [e for e in data.get("elements",[]) if e["type"]=="relation"]
    if not rels: log(f"  [ERROR] no relation for '{name}'"); return None
    rel = rels[0]; log(f"    OSM relation #{rel['id']}")
    raw = rel_to_geom(rel.get("members",[]))
    if not raw: log(f"  [ERROR] no geometry for '{name}'"); return None
    validated = validate(raw, name)
    return (validated, rel["id"]) if validated else None

def probe_admin_levels(district_bbox: str) -> list[str]:
    q = (f'[out:json][timeout:60][bbox:{district_bbox}];\n'
         'rel["boundary"="administrative"]["admin_level"];\n'
         'out tags;')
    try:
        data = overpass(q)
        levels = set()
        for e in data.get("elements",[]):
            lvl = e.get("tags",{}).get("admin_level","")
            if lvl: levels.add(lvl)
        return sorted(levels)
    except Exception as e:
        log(f"    probe failed: {e}")
        return []

def fetch_neighborhoods_bbox(district_geom: dict, district_shape,
                              admin_levels: list[str]) -> list[dict]:
    minx, miny, maxx, maxy = district_shape.bounds
    pad = 0.003
    bbox = f"{miny-pad:.5f},{minx-pad:.5f},{maxy+pad:.5f},{maxx+pad:.5f}"
    level_filter = "|".join(admin_levels)
    q = (f'[out:json][timeout:240][bbox:{bbox}];\n'
         f'rel["boundary"="administrative"]["admin_level"~"^({level_filter})$"];\n'
         'out geom;')
    log(f"    bbox={bbox}  levels={level_filter}")
    data = overpass(q)
    return [e for e in data.get("elements",[]) if e["type"]=="relation"]

def process_neighborhoods(district_name: str, district_rel_id: int,
                           district_geom: dict, district_uuid: str) -> list[dict]:
    district_shape = shape(district_geom)
    minx, miny, maxx, maxy = district_shape.bounds
    d_bbox = f"{miny:.5f},{minx:.5f},{maxy:.5f},{maxx:.5f}"

    log(f"    Probing admin levels in district bbox...")
    all_levels = probe_admin_levels(d_bbox)
    log(f"    Found admin levels: {all_levels}")
    time.sleep(REQUEST_DELAY)

    nb_levels = [l for l in all_levels if l not in ("4","5","6") and l.isdigit()]
    if not nb_levels:
        nb_levels = ["10","9","8"]
    log(f"    Will query neighborhoods at levels: {nb_levels}")

    rel_elements = fetch_neighborhoods_bbox(district_geom, district_shape, nb_levels)
    time.sleep(REQUEST_DELAY)
    log(f"    {len(rel_elements)} candidate relations")

    results: list[dict] = []
    seen: set[int] = set()
    for rel in rel_elements:
        osm_id = rel["id"]
        if osm_id in seen: continue
        seen.add(osm_id)

        osm_name = rel.get("tags",{}).get("name","")
        if not osm_name: continue

        rel_level = rel.get("tags",{}).get("admin_level","99")
        if int(rel_level) <= 6: continue

        raw = rel_to_geom(rel.get("members",[]))
        if not raw: continue
        validated = validate(raw, osm_name)
        if not validated: continue

        nb_shape = shape(validated)
        intersection = district_shape.intersection(nb_shape)
        if intersection.is_empty or intersection.area < MIN_AREA: continue

        # Clip to district — removes sea overlap / cross-border bleed
        clipped = extract_polygons(intersection)
        if clipped is None or clipped.is_empty or clipped.area < MIN_AREA:
            continue

        geojson = json.loads(json.dumps(mapping(clipped)))
        results.append({
            "osm_name": osm_name,
            "db_name":  norm(osm_name),
            "osm_id":   osm_id,
            "geojson":  geojson,
        })
        log(f"    OK '{osm_name}' (level={rel_level})")

    return results

# ---------------------------------------------------------------------------
def sq(s): return "'" + s.replace("'","''") + "'"
def jsonb(g): return sq(json.dumps(g,separators=(",",":"))) + "::jsonb"

# ---------------------------------------------------------------------------
def main():
    lines = [
        "-- V27: Real Istanbul administrative boundaries from OpenStreetMap — ALL 39 districts.",
        "-- Source: OpenStreetMap contributors, ODbL licence (https://www.openstreetmap.org).",
        "-- Generator: scripts/fetch_istanbul_boundaries.py",
        "--",
        "-- Method:",
        "--   Districts    : Overpass query admin_level=6, Istanbul bbox",
        "--   Neighborhoods: probe admin levels, bbox-scoped query, Shapely clip",
        "--   Geometry validated with Shapely make_valid + Douglas-Peucker tol=5e-5 deg",
        "--   Neighborhoods UPSERTED (INSERT ... ON CONFLICT DO UPDATE) so both",
        "--   existing rows (V5/V6/V24) and new OSM-only rows are handled.",
        "",
        "BEGIN;",
        "",
    ]
    summary = {}
    total_districts  = 0
    total_nb         = 0

    for dname, duuid in DISTRICTS.items():
        log(f"\n{'='*60}\nDistrict: {dname}\n{'='*60}")

        result = fetch_district(dname)
        time.sleep(REQUEST_DELAY)
        if not result:
            log(f"  [SKIP] {dname} — no geometry found")
            summary[dname] = []; continue

        dgeom, drelid = result
        lines += [
            f"-- District: {dname}  (OSM relation #{drelid})",
            f"UPDATE districts SET geojson_polygon = {jsonb(dgeom)}, "
            f"updated_at = now() WHERE id = {sq(duuid)};",
            "",
        ]
        total_districts += 1

        nbs = process_neighborhoods(dname, drelid, dgeom, duuid)

        if nbs:
            lines.append(f"-- Neighborhoods of {dname} ({len(nbs)} from OSM)")
            for nb in nbs:
                # Use UPSERT so new neighborhoods (not in V24 seed) are created,
                # and existing ones get their polygon refreshed.
                lines.append(
                    f"INSERT INTO neighborhoods (id, name, district_id, geojson_polygon, created_at, updated_at)"
                    f"\nVALUES (gen_random_uuid(), {sq(nb['db_name'])}, {sq(duuid)}, "
                    f"{jsonb(nb['geojson'])}, now(), now())"
                    f"\nON CONFLICT (name, district_id) DO UPDATE"
                    f"\n  SET geojson_polygon = EXCLUDED.geojson_polygon, updated_at = now();"
                )
            lines.append("")
            total_nb += len(nbs)

        summary[dname] = [nb["db_name"] for nb in nbs]

    lines += ["COMMIT;", ""]

    os.makedirs(os.path.dirname(OUT_SQL), exist_ok=True)
    with open(OUT_SQL, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))

    log(f"\nOutput: {OUT_SQL}")
    log(f"\nSummary ({total_districts} districts, {total_nb} neighborhoods):")
    for d, nbs in summary.items():
        log(f"  {d}: {len(nbs)} neighborhood(s)")
        for n in nbs: log(f"      - {n}")

if __name__ == "__main__":
    main()
