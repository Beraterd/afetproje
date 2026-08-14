#!/usr/bin/env python3
"""
fetch_district_buildings.py

Fetches real building footprints for one or more Istanbul districts from the OpenStreetMap
Overpass API, assigns each building to a neighborhood (same OSM-derived boundaries already
migrated into the app's `neighborhoods` table via V27__osm_all39_boundaries.sql — re-fetched
here from Overpass with the identical method/normalization so names match exactly), validates
geometry with Shapely, and writes a streamable NDJSON data file per district for the backend's
BuildingImportService.

Generalized from the original Pendik-only pilot script (fetch_pendik_buildings.py) — the
Overpass/retry/geometry-validation/neighborhood-assignment logic is unchanged, only the district
name is now a parameter instead of a hardcoded constant, and each district gets its own output
file so importing one district never depends on or clobbers another (see
BuildingImportService.importFromClasspath(resourcePath, districtName), AdminBuildingController).

DATA SOURCE:
    OpenStreetMap contributors — https://www.openstreetmap.org
    License: ODbL  https://opendatacommons.org/licenses/odbl/

REQUIREMENTS:
    pip install requests shapely

USAGE:
    python scripts/fetch_district_buildings.py Kadıköy
    python scripts/fetch_district_buildings.py Kadıköy Kartal Beykoz Adalar   # sequential, one
                                                                               # Overpass fetch
                                                                               # per district; a
                                                                               # failure in one
                                                                               # district does not
                                                                               # abort the rest

    # Output: backend/src/main/resources/data/buildings/{slug}.ndjson (slug = ASCII-folded
    # lowercase district name, e.g. "Kadıköy" -> "kadikoy" — same folding rule as the backend's
    # TurkishTextNormalizer, so AdminBuildingController can derive the same filename from
    # District.name at import time).
    # These files are checked into the repo (not regenerated at deploy/CI time — Overpass is slow
    # and rate-limited for dense districts). Re-run manually and commit the refreshed file(s) when
    # a district needs updated OSM data.
"""

import json, os, sys, time
from typing import Optional
import requests
from shapely.geometry import shape, mapping, Point, Polygon, MultiPolygon, GeometryCollection
from shapely.ops import unary_union
from shapely.validation import make_valid

# ---------------------------------------------------------------------------
OVERPASS_ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass.openstreetmap.ru/api/interpreter",
]
REQUEST_DELAY = 5
RETRY_DELAY = 30
SIMPLIFY_TOL = 0.00005  # ~5m, same tolerance as fetch_istanbul_boundaries.py
MIN_AREA = 5e-8

LEVEL_HEIGHT_M_DEFAULT = 3.0  # kept in sync conceptually with backend app.building3d.level-height-m;
                               # the script does NOT precompute estimatedHeightM/heightSource —
                               # the backend importer does, using its own configurable constant.

# Canonical District.name -> the exact OSM `name` tag value, for districts where they differ.
# Used ONLY to build Overpass query strings (rel["name"=...]/area["name"=...]); every other use
# of the district name (ndjson displayAddress, DB lookups, neighborhoodName matching) keeps the
# canonical name unchanged. Discovered case: OSM's admin_level=6 relation for Kağıthane
# (relation 1765894) is tagged with the old orthography name="Kâğıthane" (circumflex â), not the
# modern "Kağıthane" this app and Turkey's official TÜİK/İçişleri Bakanlığı sources use — an
# exact-name Overpass query for "Kağıthane" legitimately returns zero relations. Add further
# entries here if another district is found with the same kind of OSM/canonical name mismatch;
# this is intentionally a lookup table, not a per-district branch in the fetch logic.
OSM_NAME_ALIASES = {
    "Kağıthane": "Kâğıthane",
}


def osm_query_name(district_name: str) -> str:
    return OSM_NAME_ALIASES.get(district_name, district_name)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUT_DIR = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "data", "buildings")

_ep_idx = 0
def log(m): print(m, file=sys.stderr, flush=True)

def slugify(name: str) -> str:
    """Same character-folding rule as the backend's TurkishTextNormalizer: İ/I/ı/i->i, Ş->s,
    Ç->c, Ğ->g, Ö->o, Ü->u, lowercase, non-alnum stripped. District names here are single
    words, so no separator handling is needed."""
    fold = {
        "İ": "i", "I": "i", "ı": "i", "i": "i",
        "Ş": "s", "ş": "s",
        "Ç": "c", "ç": "c",
        "Ğ": "g", "ğ": "g",
        "Ö": "o", "ö": "o",
        "Ü": "u", "ü": "u",
    }
    out = "".join(fold.get(c, c) for c in name)
    out = out.lower()
    return "".join(c for c in out if c.isalnum())

def overpass(query: str, timeout: int = 300) -> dict:
    global _ep_idx
    for attempt in range(len(OVERPASS_ENDPOINTS) * 5):
        url = OVERPASS_ENDPOINTS[_ep_idx % len(OVERPASS_ENDPOINTS)]
        try:
            log(f"    [{url.split('/')[2][:20]}] attempt {attempt+1}")
            # NOTE: do NOT send an Accept-Charset header — overpass-api.de's Apache frontend
            # returns 406 Not Acceptable for it (discovered empirically). A normal UA avoids
            # being treated as an abusive default client.
            resp = requests.post(url, data={"data": query}, timeout=timeout + 30,
                                  headers={"User-Agent": "afet-koordinasyon-district-buildings/1.0"})
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
def _eq(a, b, tol=1e-7): return abs(a[0]-b[0]) < tol and abs(a[1]-b[1]) < tol

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
    outers = [m for m in members if m.get("role") == "outer" and "geometry" in m]
    inners = [m for m in members if m.get("role") == "inner" and "geometry" in m]
    outer_rings = assemble_rings(outers)
    inner_rings = assemble_rings(inners)
    if not outer_rings: return None
    if len(outer_rings) == 1:
        return {"type": "Polygon", "coordinates": [outer_rings[0]] + inner_rings}
    outer_shapes = [Polygon(r) for r in outer_rings]
    polys = []
    for idx, outer in enumerate(outer_rings):
        inners_assigned = [ir for ir in inner_rings
                            if outer_shapes[idx].contains(Polygon(ir).centroid)]
        polys.append([outer] + inners_assigned)
    return {"type": "MultiPolygon", "coordinates": polys}

def extract_polygons(geom):
    if isinstance(geom, (Polygon, MultiPolygon)): return geom
    if isinstance(geom, GeometryCollection):
        parts = [g for g in geom.geoms if isinstance(g, (Polygon, MultiPolygon))]
        return unary_union(parts) if parts else None
    return None

def validate_polygon(raw: dict, label: str) -> Optional[dict]:
    try: geom = shape(raw)
    except Exception as e: log(f"    [SKIP] parse error '{label}': {e}"); return None
    if not geom.is_valid: geom = make_valid(geom)
    geom = extract_polygons(geom)
    if geom is None or geom.is_empty or geom.area < MIN_AREA: return None
    geom = geom.simplify(SIMPLIFY_TOL, preserve_topology=True)
    return json.loads(json.dumps(mapping(geom)))

def norm(name: str) -> str:
    """Identical normalization to fetch_istanbul_boundaries.py's norm() — keeps neighborhood
    names consistent with what's already in the `neighborhoods` table (V27)."""
    name = name.strip()
    for sfx in [" Mahallesi", " Mah.", " Mh.", " mahallesi"]:
        if name.lower().endswith(sfx.lower()):
            name = name[:-len(sfx)].strip(); break
    return name

# ---------------------------------------------------------------------------
def fetch_district_polygon(name: str):
    query_name = osm_query_name(name)
    q = (f'[out:json][timeout:90];\n'
         f'rel["name"="{query_name}"]["admin_level"="6"]["boundary"="administrative"];\n'
         'out geom;')
    log(f"Fetching district polygon: {name}" + (f" (OSM name: {query_name})" if query_name != name else ""))
    data = overpass(q, timeout=90)
    rels = [e for e in data.get("elements", []) if e["type"] == "relation"]
    if not rels: raise RuntimeError(f"No OSM relation found for district '{name}'")
    raw = rel_to_geom(rels[0].get("members", []))
    if not raw: raise RuntimeError(f"No geometry assembled for district '{name}'")
    validated = validate_polygon(raw, name)
    if not validated: raise RuntimeError(f"Geometry validation failed for district '{name}'")
    return shape(validated)

def probe_admin_levels(bbox: str) -> list:
    q = f'[out:json][timeout:60][bbox:{bbox}];\nrel["boundary"="administrative"]["admin_level"];\nout tags;'
    data = overpass(q, timeout=60)
    levels = set()
    for e in data.get("elements", []):
        lvl = e.get("tags", {}).get("admin_level", "")
        if lvl: levels.add(lvl)
    return sorted(levels)

def fetch_neighborhoods(district_name: str, district_shape) -> list:
    minx, miny, maxx, maxy = district_shape.bounds
    pad = 0.003
    d_bbox = f"{miny:.5f},{minx:.5f},{maxy:.5f},{maxx:.5f}"

    log(f"Probing admin levels within {district_name} bbox...")
    all_levels = probe_admin_levels(d_bbox)
    time.sleep(REQUEST_DELAY)
    nb_levels = [l for l in all_levels if l not in ("4", "5", "6") and l.isdigit()]
    if not nb_levels: nb_levels = ["10", "9", "8"]
    log(f"  Neighborhood admin levels: {nb_levels}")

    level_filter = "|".join(nb_levels)
    q = (f'[out:json][timeout:180][bbox:{d_bbox}];\n'
         f'rel["boundary"="administrative"]["admin_level"~"^({level_filter})$"];\n'
         'out geom;')
    data = overpass(q, timeout=180)
    time.sleep(REQUEST_DELAY)

    results = []
    seen = set()
    for rel in data.get("elements", []):
        if rel["type"] != "relation": continue
        osm_id = rel["id"]
        if osm_id in seen: continue
        seen.add(osm_id)
        osm_name = rel.get("tags", {}).get("name", "")
        if not osm_name: continue
        rel_level = rel.get("tags", {}).get("admin_level", "99")
        if int(rel_level) <= 6: continue

        raw = rel_to_geom(rel.get("members", []))
        if not raw: continue
        validated = validate_polygon(raw, osm_name)
        if not validated: continue

        nb_shape = shape(validated)
        intersection = district_shape.intersection(nb_shape)
        if intersection.is_empty or intersection.area < MIN_AREA: continue
        clipped = extract_polygons(intersection)
        if clipped is None or clipped.is_empty or clipped.area < MIN_AREA: continue

        results.append({"name": norm(osm_name), "shape": clipped})
        log(f"  OK neighborhood '{osm_name}' -> '{norm(osm_name)}'")
    return results

def fetch_buildings(district_name: str) -> list:
    query_name = osm_query_name(district_name)
    q = (f'[out:json][timeout:400];\n'
         f'area["name"="{query_name}"]["admin_level"="6"]["boundary"="administrative"]->.a;\n'
         f'way(area.a)["building"];\n'
         'out geom;')
    log(f"Fetching buildings for {district_name} (this can take several minutes)...")
    data = overpass(q, timeout=400)
    ways = [e for e in data.get("elements", []) if e["type"] == "way"]
    log(f"  {len(ways)} building ways returned")
    return ways

def parse_height_m(tags: dict) -> Optional[float]:
    raw = tags.get("height")
    if not raw: return None
    raw = raw.strip().rstrip("m").strip().replace(",", ".")
    try:
        v = float(raw)
        return v if 0 < v < 500 else None
    except ValueError:
        return None

def parse_levels(tags: dict) -> Optional[int]:
    raw = tags.get("building:levels")
    if not raw: return None
    try:
        v = float(raw.strip())
        v = int(round(v))
        return v if 0 < v < 200 else None
    except ValueError:
        return None

def build_display_address(street: Optional[str], number: Optional[str], neighborhood: str, district_name: str) -> str:
    parts = []
    if street:
        parts.append(f"{street}{(' ' + number) if number else ''}")
    parts.append(f"{neighborhood} Mah.")
    parts.append(f"{district_name}/İstanbul")
    return ", ".join(parts)

# ---------------------------------------------------------------------------
def fetch_one_district(district_name: str) -> None:
    slug = slugify(district_name)
    out_path = os.path.join(OUT_DIR, f"{slug}.ndjson")

    district_shape = fetch_district_polygon(district_name)
    neighborhoods = fetch_neighborhoods(district_name, district_shape)
    if not neighborhoods:
        raise RuntimeError(f"No {district_name} neighborhoods resolved — aborting (would misassign every building)")
    log(f"Resolved {len(neighborhoods)} {district_name} neighborhoods")

    ways = fetch_buildings(district_name)

    os.makedirs(OUT_DIR, exist_ok=True)
    written = 0
    skipped_no_geom = 0
    skipped_no_neighborhood = 0
    unmatched_sample = []

    with open(out_path, "w", encoding="utf-8") as fh:
        for way in ways:
            geom_pts = way.get("geometry")
            if not geom_pts or len(geom_pts) < 4:
                skipped_no_geom += 1; continue
            ring = [(p["lon"], p["lat"]) for p in geom_pts]
            if not _eq(ring[0], ring[-1]): ring.append(ring[0])
            try:
                poly = Polygon(ring)
                if not poly.is_valid: poly = make_valid(poly)
                poly = extract_polygons(poly)
            except Exception:
                skipped_no_geom += 1; continue
            if poly is None or poly.is_empty or poly.area <= 0:
                skipped_no_geom += 1; continue

            centroid = poly.centroid
            neighborhood_name = None
            for nb in neighborhoods:
                if nb["shape"].contains(centroid):
                    neighborhood_name = nb["name"]; break
            if neighborhood_name is None:
                skipped_no_neighborhood += 1
                if len(unmatched_sample) < 10:
                    unmatched_sample.append(way["id"])
                continue

            tags = way.get("tags", {})
            street = tags.get("addr:street")
            number = tags.get("addr:housenumber")
            minx, miny, maxx, maxy = poly.bounds

            record = {
                "externalId": f"way/{way['id']}",
                "neighborhoodName": neighborhood_name,
                "streetName": street,
                "buildingNumber": number,
                "displayAddress": build_display_address(street, number, neighborhood_name, district_name),
                "latitude": round(centroid.y, 7),
                "longitude": round(centroid.x, 7),
                "geometry": mapping(poly),
                "heightM": parse_height_m(tags),
                "levels": parse_levels(tags),
                "buildingType": tags.get("building") if tags.get("building") != "yes" else None,
                "name": tags.get("name"),
                "bboxMinLat": round(miny, 7),
                "bboxMaxLat": round(maxy, 7),
                "bboxMinLon": round(minx, 7),
                "bboxMaxLon": round(maxx, 7),
            }
            fh.write(json.dumps(record, ensure_ascii=False) + "\n")
            written += 1

    log("")
    log(f"Output: {out_path}")
    log(f"Written: {written}")
    log(f"Skipped (no/invalid geometry): {skipped_no_geom}")
    log(f"Skipped (no matching neighborhood): {skipped_no_neighborhood}")
    if unmatched_sample:
        log(f"  sample unmatched way ids: {unmatched_sample}")

def main():
    districts = sys.argv[1:]
    if not districts:
        log("Usage: python scripts/fetch_district_buildings.py <District1> [District2] ...")
        sys.exit(1)

    results = {}
    for i, d in enumerate(districts):
        log(f"\n{'='*60}\n{d}\n{'='*60}")
        try:
            fetch_one_district(d)
            results[d] = "OK"
        except Exception as e:
            log(f"[FAILED] {d}: {e}")
            results[d] = f"FAILED: {e}"
        if i < len(districts) - 1:
            time.sleep(REQUEST_DELAY)

    log(f"\n{'='*60}\nSummary\n{'='*60}")
    for d, status in results.items():
        log(f"  {d}: {status}")

    if any(s != "OK" for s in results.values()):
        sys.exit(1)

if __name__ == "__main__":
    main()
