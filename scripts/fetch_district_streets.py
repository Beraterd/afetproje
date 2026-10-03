#!/usr/bin/env python3
"""
fetch_district_streets.py

Fetches real named street/road geometry for one or more Istanbul districts from the
OpenStreetMap Overpass API, clips each way to this project's OWN neighborhood polygons
(read directly from the running DB via `docker exec ... psql`, NOT a fresh OSM boundary
fetch — guarantees the street->neighborhood assignment is spatially consistent with
whatever the app already uses for everything else), merges every way sharing the same
`name` tag within the same neighborhood into a single logical street (MultiLineString),
and writes a streamable NDJSON data file per district for the backend's
StreetImportService (sibling of fetch_district_buildings.py -> BuildingImportService).

DATA SOURCE:
    OpenStreetMap contributors — https://www.openstreetmap.org
    License: ODbL  https://opendatacommons.org/licenses/odbl/

WHICH ROADS: named highway=* ways only, restricted to types that are meaningful for
address/street search (residential/tertiary/secondary/primary/*_link/trunk/trunk_link/
unclassified/living_street/pedestrian). Deliberately EXCLUDES footway/path/steps/
cycleway/service/track/motorway — not "cadde/sokak/bulvar" in the sense a user searching
an address means (see item 5 of the feature spec this script was built for).

REQUIREMENTS:
    pip install requests shapely
    Docker container `afet-postgres` running and reachable (reads district_id/
    neighborhood polygons directly from the DB — read-only, `\\COPY ... TO STDOUT`).

USAGE:
    python scripts/fetch_district_streets.py Kadıköy
    python scripts/fetch_district_streets.py Kadıköy Kartal Beykoz   # sequential

    # Output: backend/src/main/resources/data/streets/{slug}.ndjson (same slugify rule as
    # fetch_district_buildings.py, matching TurkishTextNormalizer.normalize() on the Java
    # side so AdminStreetController can derive the same filename from District.name).
"""

import csv
import io
import json
import os
import subprocess
import sys
import time

import requests
from shapely.geometry import shape, mapping, LineString, MultiLineString
from shapely.ops import linemerge

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import fetch_district_buildings as fdb  # reuse overpass()/osm_query_name()/slugify()/REQUEST_DELAY

# ---------------------------------------------------------------------------
# Highway types considered meaningful "cadde/sokak/bulvar" address-search roads.
NAMED_ROAD_TYPES = [
    "residential", "tertiary", "secondary", "primary",
    "primary_link", "secondary_link", "tertiary_link",
    "trunk", "trunk_link", "unclassified", "living_street", "pedestrian",
]
MIN_SEGMENT_LENGTH_DEG = 1e-6  # ~0.1m at Istanbul's latitude — filters pure-touching-point clips

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
OUT_DIR = os.path.join(PROJECT_ROOT, "backend", "src", "main", "resources", "data", "streets")


def log(m):
    print(m, file=sys.stderr, flush=True)


# ---------------------------------------------------------------------------
def psql_copy(query: str) -> list[dict]:
    cmd = [
        "docker", "exec", "afet-postgres", "psql", "-U", "afetuser", "-d", "afetdb",
        "-c", f"\\COPY ({query}) TO STDOUT WITH (FORMAT csv, HEADER true)",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", timeout=120)
    if result.returncode != 0:
        raise RuntimeError(f"psql_copy failed: {result.stderr}")
    reader = csv.DictReader(io.StringIO(result.stdout))
    return list(reader)


def get_district_id(district_name: str) -> str:
    escaped = district_name.replace("'", "''")
    rows = psql_copy(f"SELECT id FROM districts WHERE name = '{escaped}'")
    if not rows:
        raise RuntimeError(f"District '{district_name}' not found in DB — apply boundary migrations first")
    return rows[0]["id"]


def get_neighborhoods(district_id: str) -> list[dict]:
    """Reads this project's OWN neighborhood polygons — not a fresh OSM fetch. See module
    docstring for why: guarantees street->neighborhood assignment matches everything else
    the app already draws with this exact geometry."""
    rows = psql_copy(
        "SELECT id, name, geojson_polygon::text AS geojson_polygon FROM neighborhoods "
        f"WHERE district_id = '{district_id}' AND geojson_polygon IS NOT NULL"
    )
    result = []
    for r in rows:
        try:
            geom = shape(json.loads(r["geojson_polygon"]))
            if not geom.is_valid or geom.is_empty:
                continue
            result.append({"id": r["id"], "name": r["name"], "shape": geom})
        except Exception as e:
            log(f"  [SKIP] neighborhood '{r['name']}' geometry parse failed: {e}")
    return result


# ---------------------------------------------------------------------------
def fetch_named_roads(district_name: str) -> list[dict]:
    query_name = fdb.osm_query_name(district_name)
    type_filter = "|".join(NAMED_ROAD_TYPES)
    q = (
        f'[out:json][timeout:180];\n'
        f'area["name"="{query_name}"]["admin_level"="6"]["boundary"="administrative"]->.a;\n'
        f'way(area.a)["highway"~"^({type_filter})$"]["name"];\n'
        'out geom;'
    )
    log(f"Fetching named roads for {district_name}" + (f" (OSM name: {query_name})" if query_name != district_name else ""))
    data = fdb.overpass(q, timeout=180)
    ways = [e for e in data.get("elements", []) if e["type"] == "way"]
    log(f"  {len(ways)} named road ways returned")
    return ways


def way_to_linestring(way: dict) -> LineString | None:
    pts = way.get("geometry")
    if not pts or len(pts) < 2:
        return None
    coords = [(p["lon"], p["lat"]) for p in pts]
    try:
        line = LineString(coords)
        return line if line.is_valid and not line.is_empty and line.length > 0 else None
    except Exception:
        return None


def clip_lines_to_geom(geom):
    """Normalizes a shapely intersection result (LineString/MultiLineString/GeometryCollection/
    Point) down to a flat list of LineStrings — an intersection can degrade to a Point where a
    road merely touches a neighborhood's border, which we don't want to keep."""
    if geom is None or geom.is_empty:
        return []
    if geom.geom_type == "LineString":
        return [geom] if geom.length >= MIN_SEGMENT_LENGTH_DEG else []
    if geom.geom_type == "MultiLineString":
        return [g for g in geom.geoms if g.length >= MIN_SEGMENT_LENGTH_DEG]
    if geom.geom_type == "GeometryCollection":
        out = []
        for g in geom.geoms:
            out.extend(clip_lines_to_geom(g))
        return out
    return []  # Point/MultiPoint — a road that only touches the boundary at one vertex


# ---------------------------------------------------------------------------
def fetch_one_district(district_name: str) -> None:
    slug = fdb.slugify(district_name)
    out_path = os.path.join(OUT_DIR, f"{slug}.ndjson")

    district_id = get_district_id(district_name)
    neighborhoods = get_neighborhoods(district_id)
    if not neighborhoods:
        raise RuntimeError(f"No {district_name} neighborhoods with geometry in DB — aborting")
    log(f"Loaded {len(neighborhoods)} {district_name} neighborhoods from DB")

    ways = fetch_named_roads(district_name)

    # groups[(neighborhood_id, name)] -> {lines: [...], road_type, external_id, neighborhood_name}
    groups: dict[tuple, dict] = {}
    skipped_no_geom = 0
    skipped_no_neighborhood_overlap = 0

    for way in ways:
        name = way.get("tags", {}).get("name")
        road_type = way.get("tags", {}).get("highway")
        line = way_to_linestring(way)
        if line is None or not name:
            skipped_no_geom += 1
            continue

        matched_any = False
        for nb in neighborhoods:
            try:
                clipped = line.intersection(nb["shape"])
            except Exception:
                continue
            segments = clip_lines_to_geom(clipped)
            if not segments:
                continue
            matched_any = True
            key = (nb["id"], name)
            g = groups.get(key)
            if g is None:
                g = {"lines": [], "road_type": road_type, "external_id": f"way/{way['id']}",
                     "neighborhood_name": nb["name"]}
                groups[key] = g
            g["lines"].extend(segments)
            if g["road_type"] is None and road_type:
                g["road_type"] = road_type

        if not matched_any:
            skipped_no_neighborhood_overlap += 1

    os.makedirs(OUT_DIR, exist_ok=True)
    written = 0
    with open(out_path, "w", encoding="utf-8") as fh:
        for (neighborhood_id, name), g in groups.items():
            lines = g["lines"]
            merged = linemerge(lines) if len(lines) > 1 else lines[0]
            multi = merged if merged.geom_type == "MultiLineString" else MultiLineString([merged])

            minx, miny, maxx, maxy = multi.bounds
            center = multi.centroid

            record = {
                "externalId": g["external_id"],
                "name": name,
                "neighborhoodName": g["neighborhood_name"],
                "roadType": g["road_type"],
                "geometry": mapping(multi),
                "bboxMinLat": round(miny, 7),
                "bboxMaxLat": round(maxy, 7),
                "bboxMinLon": round(minx, 7),
                "bboxMaxLon": round(maxx, 7),
                "centerLat": round(center.y, 7),
                "centerLon": round(center.x, 7),
            }
            fh.write(json.dumps(record, ensure_ascii=False) + "\n")
            written += 1

    log("")
    log(f"Output: {out_path}")
    log(f"Logical streets written: {written}")
    log(f"Raw ways skipped (no geometry/name): {skipped_no_geom}")
    log(f"Raw ways with no neighborhood overlap: {skipped_no_neighborhood_overlap}")


def main():
    districts = sys.argv[1:]
    if not districts:
        log("Usage: python scripts/fetch_district_streets.py <District1> [District2] ...")
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
            time.sleep(fdb.REQUEST_DELAY)

    log(f"\n{'='*60}\nSummary\n{'='*60}")
    for d, status in results.items():
        log(f"  {d}: {status}")

    if any(s != "OK" for s in results.values()):
        sys.exit(1)


if __name__ == "__main__":
    main()
