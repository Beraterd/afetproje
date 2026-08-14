#!/usr/bin/env python3
"""
audit_european_geodata.py

Read-only geographic correctness audit for the 25 European-side ("Avrupa
yakası") districts: Arnavutköy, Avcılar, Bağcılar, Bahçelievler, Bakırköy,
Başakşehir, Bayrampaşa, Beşiktaş, Beylikdüzü, Beyoğlu, Büyükçekmece,
Çatalca, Esenler, Esenyurt, Eyüpsultan, Fatih, Gaziosmanpaşa, Güngören,
Kağıthane, Küçükçekmece, Sarıyer, Silivri, Sultangazi, Şişli, Zeytinburnu.

Sibling script to scripts/audit_anatolian_geodata.py — identical logic and
checks, only DISTRICTS_EUROPEAN and the report output path differ (kept as
a separate script rather than a shared CLI flag so each side's audit has
its own docstring/district list that doesn't need a --side flag to read
correctly, matching how fetch_district_buildings.py/fetch_istanbul_
boundaries.py already treat "which districts" as script-level data, not a
runtime toggle).

Checks the full chain: district geometry -> neighborhood geometry (parent
containment, overlap, sliver detection) -> building spatial association
(district FK consistency, neighborhood spatial match via stored centroid,
Istanbul-bounds sanity, geometry validity). NOTE: as of this script's
creation, no building data has been imported for the European side yet
(backend/src/main/resources/data/buildings/ only has the 14 Anatolian-side
ndjson files) — the building-related checks below will correctly report
zero buildings per district until that import happens; that is expected,
not a bug in this script.

DATA SOURCE FOR GROUND TRUTH:
    OpenStreetMap (Overpass API) — the SAME source and method already used
    by this project to seed districts/neighborhoods.geojson_polygon
    (scripts/fetch_istanbul_boundaries.py -> V27__osm_all39_boundaries.sql)
    and to fetch/import building data (scripts/fetch_district_buildings.py).
    This script reuses fetch_district_buildings.py's fetch_district_polygon()
    and fetch_neighborhoods() so the audit's "ground truth" polygons are
    derived identically to how the app's own data pipeline derives them.
    Fresh fetches are cached under scripts/.audit_cache/<slug>_ground_truth.json
    so a re-run does NOT re-hit Overpass unless --refresh-cache is passed.

NO POSTGIS: this deployment stores geometry as GeoJSON in a jsonb column
(see backend Building.java / District.java / Neighborhood.java doc
comments). There is no ST_IsValid/ST_Within/etc. available in the DB, so
this script pulls geometry out via `docker exec ... psql` COPY ... CSV
(no new Python dependency — psycopg2 is NOT installed in this environment
and is deliberately not added; the existing fetch scripts' convention of
"requests + shapely only" is kept) and does all geometric validation
client-side with Shapely, which mirrors PostGIS semantics closely enough
for this audit (is_valid, area, representative_point ~ ST_PointOnSurface,
intersection-area-ratio ~ overlap fraction).

BOUNDARY TOLERANCE: buildings/polygons within SLIVER_TOL degrees of a
neighborhood boundary are not auto-flagged as "outside" on a hairline
technicality. SLIVER_TOL reuses SIMPLIFY_TOL = 0.00005 deg (~5m), the same
constant scripts/fetch_district_buildings.py and fetch_istanbul_boundaries.py
already use for Douglas-Peucker simplification — chosen here for consistency
with the project's existing "how close is close enough" convention, not
invented fresh for this script.

SPATIAL MATCH TEST POINT: uses the building's stored centroid (latitude/
longitude), matching exactly what BuildingImportService/fetch_district_
buildings.py use to decide neighborhood membership at import time — see
scripts/audit_anatolian_geodata.py's audit_building_spatial_match()
docstring for why representative_point() was tried and rejected there
(6 false positives across 313k Anatolian-side buildings, all resolved once
switched to centroid). This script uses the same centroid-based test from
the start.

USAGE:
    python scripts/audit_european_geodata.py                 # full audit, uses cache
    python scripts/audit_european_geodata.py --refresh-cache  # re-fetch OSM ground truth
    python scripts/audit_european_geodata.py --districts Fatih,Şişli  # subset, for iteration

OUTPUT: writes a full JSON report to scripts/.audit_cache/audit_report_european.json
and a human-readable summary to stdout. READ-ONLY — never writes to the DB.
"""

import csv
import io
import json
import os
import subprocess
import sys
import time
from collections import defaultdict
from typing import Optional

from shapely.geometry import shape, mapping, Point, Polygon, MultiPolygon
from shapely.strtree import STRtree
from shapely.validation import make_valid, explain_validity

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import fetch_district_buildings as fdb  # reuse the project's own OSM fetch logic

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CACHE_DIR = os.path.join(SCRIPT_DIR, ".audit_cache")
REPORT_PATH = os.path.join(CACHE_DIR, "audit_report_european.json")

DISTRICTS_EUROPEAN = [
    "Arnavutköy", "Avcılar", "Bağcılar", "Bahçelievler", "Bakırköy",
    "Başakşehir", "Bayrampaşa", "Beşiktaş", "Beylikdüzü", "Beyoğlu",
    "Büyükçekmece", "Çatalca", "Esenler", "Esenyurt", "Eyüpsultan", "Fatih",
    "Gaziosmanpaşa", "Güngören", "Kağıthane", "Küçükçekmece", "Sarıyer",
    "Silivri", "Sultangazi", "Şişli", "Zeytinburnu",
]

SLIVER_TOL = fdb.SIMPLIFY_TOL  # ~5m, reused from the project's own fetch scripts
# Istanbul province rough bounding box (generous pad around the whole province,
# not just European side, so this only catches gross errors like lat/lon swap
# or a building placed in a totally different country/region).
ISTANBUL_BBOX = (27.8, 40.35, 30.0, 41.65)  # (minlon, minlat, maxlon, maxlat)


def log(msg):
    print(msg, file=sys.stderr, flush=True)


# ---------------------------------------------------------------------------
# DB extraction — zero new dependencies: shell out to the already-running
# afet-postgres container via `docker exec ... psql \COPY ... CSV`.
# ---------------------------------------------------------------------------
def psql_copy(query: str) -> list[dict]:
    cmd = [
        "docker", "exec", "afet-postgres", "psql", "-U", "afetuser", "-d", "afetdb",
        "-c", f"\\COPY ({query}) TO STDOUT WITH (FORMAT csv, HEADER true)",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", timeout=300)
    if result.returncode != 0:
        raise RuntimeError(f"psql_copy failed: {result.stderr}")
    reader = csv.DictReader(io.StringIO(result.stdout))
    return list(reader)


# ---------------------------------------------------------------------------
# Geometry parsing — Polygon + MultiPolygon, null/invalid handling.
# ---------------------------------------------------------------------------
def parse_geometry(raw: Optional[str], label: str, issues: list) -> Optional[object]:
    if raw is None or raw.strip() == "":
        issues.append(("NULL_GEOMETRY", label))
        return None
    try:
        gj = json.loads(raw)
    except Exception as e:
        issues.append(("INVALID_JSON", f"{label}: {e}"))
        return None
    gtype = gj.get("type")
    if gtype not in ("Polygon", "MultiPolygon"):
        issues.append(("UNEXPECTED_GEOM_TYPE", f"{label}: {gtype}"))
        return None
    try:
        geom = shape(gj)
    except Exception as e:
        issues.append(("SHAPELY_PARSE_ERROR", f"{label}: {e}"))
        return None
    if geom.is_empty:
        issues.append(("EMPTY_GEOMETRY", label))
        return None
    if not geom.is_valid:
        reason = explain_validity(geom)
        fixed = make_valid(geom)
        issues.append(("INVALID_GEOMETRY_AUTOFIXED_FOR_ANALYSIS_ONLY", f"{label}: {reason}"))
        geom = fixed
    return geom


def is_sliver(geom, min_area_deg2=1e-7, min_vertices=5) -> bool:
    """Heuristic: tiny area AND few vertices -> looks like a ring-assembly
    fragment rather than a real administrative polygon. Both conditions are
    required so a genuinely small-but-real neighborhood isn't misflagged."""
    if geom is None:
        return False
    try:
        nverts = sum(len(p.exterior.coords) for p in (geom.geoms if hasattr(geom, "geoms") else [geom]))
    except Exception:
        nverts = 999
    return geom.area < min_area_deg2 and nverts < min_vertices


def bbox_of(geom):
    minx, miny, maxx, maxy = geom.bounds
    return {"minLon": minx, "minLat": miny, "maxLon": maxx, "maxLat": maxy}


# ---------------------------------------------------------------------------
# Fresh OSM ground truth, cached per district.
# ---------------------------------------------------------------------------
def load_or_fetch_osm_ground_truth(district_name: str, refresh: bool) -> Optional[dict]:
    slug = fdb.slugify(district_name)
    cache_path = os.path.join(CACHE_DIR, f"{slug}_ground_truth.json")
    if not refresh and os.path.exists(cache_path):
        with open(cache_path, "r", encoding="utf-8") as f:
            cached = json.load(f)
        log(f"  [cache] {district_name} ground truth loaded from {cache_path}")
        return cached

    log(f"  [OSM] Fetching fresh ground truth for {district_name}...")
    try:
        district_shape = fdb.fetch_district_polygon(district_name)
        neighborhoods = fdb.fetch_neighborhoods(district_name, district_shape)
    except Exception as e:
        log(f"  [OSM] FAILED for {district_name}: {e}")
        return None

    data = {
        "district_geojson": mapping(district_shape),
        "neighborhoods": [
            {"name": nb["name"], "geojson": mapping(nb["shape"])} for nb in neighborhoods
        ],
    }
    os.makedirs(CACHE_DIR, exist_ok=True)
    with open(cache_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False)
    time.sleep(fdb.REQUEST_DELAY)
    return data


# ---------------------------------------------------------------------------
# Main audit
# ---------------------------------------------------------------------------
def audit_districts(district_names: list[str]) -> dict:
    log("=== Loading district rows from DB ===")
    rows = psql_copy(
        "SELECT id, name, geojson_polygon::text AS geojson_polygon FROM districts "
        "WHERE name IN (" + ",".join(f"'{n}'" for n in district_names) + ")"
    )
    by_name = {r["name"]: r for r in rows}

    results = {}
    for name in district_names:
        row = by_name.get(name)
        entry = {"district": name, "issues": [], "status": "OK"}
        if row is None:
            entry["status"] = "CONFIRMED_ERROR"
            entry["issues"].append(("MISSING_DB_ROW", name))
            results[name] = entry
            continue

        issues = []
        geom = parse_geometry(row["geojson_polygon"], f"district:{name}", issues)
        entry["issues"] = issues
        entry["geometry_type"] = geom.geom_type if geom is not None else None
        entry["is_multipolygon"] = geom.geom_type == "MultiPolygon" if geom else None
        if geom is not None:
            entry["area_deg2"] = geom.area
            entry["bbox"] = bbox_of(geom)
            entry["valid_after_parse"] = True
        entry["_shape"] = geom
        results[name] = entry
    return results


def audit_neighborhoods(district_names: list[str]) -> dict:
    log("=== Loading neighborhood rows from DB ===")
    rows = psql_copy(
        "SELECT n.id, n.name, n.district_id, d.name AS district_name, "
        "n.geojson_polygon::text AS geojson_polygon "
        "FROM neighborhoods n JOIN districts d ON d.id = n.district_id "
        "WHERE d.name IN (" + ",".join(f"'{n}'" for n in district_names) + ") "
        "ORDER BY d.name, n.name"
    )
    log(f"  {len(rows)} neighborhoods across {len(district_names)} districts")

    by_district = defaultdict(list)
    for r in rows:
        by_district[r["district_name"]].append(r)
    return by_district


def audit_building_fk_consistency(district_names: list[str]) -> list[dict]:
    log("=== Building FK consistency check (building.district_id vs neighborhood.district_id) ===")
    rows = psql_copy(
        "SELECT b.id, d.name AS building_district, nd.name AS neighborhood_parent_district, "
        "b.neighborhood_id, n.name AS neighborhood_name "
        "FROM buildings b "
        "JOIN districts d ON d.id = b.district_id "
        "JOIN neighborhoods n ON n.id = b.neighborhood_id "
        "JOIN districts nd ON nd.id = n.district_id "
        "WHERE d.name IN (" + ",".join(f"'{n}'" for n in district_names) + ") "
        "AND d.id <> nd.id"
    )
    log(f"  {len(rows)} FK mismatches found")
    return rows


def audit_building_bounds_sanity(district_names: list[str]) -> list[dict]:
    log("=== Building Istanbul-bounds sanity check (all buildings, full coverage) ===")
    minlon, minlat, maxlon, maxlat = ISTANBUL_BBOX
    rows = psql_copy(
        "SELECT b.id, d.name AS district, n.name AS neighborhood, b.latitude, b.longitude "
        "FROM buildings b JOIN districts d ON d.id=b.district_id JOIN neighborhoods n ON n.id=b.neighborhood_id "
        "WHERE d.name IN (" + ",".join(f"'{n}'" for n in district_names) + ") "
        f"AND (b.longitude < {minlon} OR b.longitude > {maxlon} OR b.latitude < {minlat} OR b.latitude > {maxlat})"
    )
    log(f"  {len(rows)} buildings outside Istanbul bbox {ISTANBUL_BBOX}")
    return rows


def audit_building_spatial_match(district_name: str, ground_truth: dict, db_neighborhoods: list[dict]) -> dict:
    """For one district: build an STRtree from the FRESH OSM neighborhood
    polygons (ground truth), then test every building's point against it.
    Compares against building.neighborhoodId (by name, since ground-truth
    polygons aren't persisted with DB IDs). Test point is the building's
    stored centroid (latitude/longitude) — see module docstring."""
    if ground_truth is None:
        return {"district": district_name, "skipped": "no ground truth (OSM fetch failed)"}

    gt_polys = []
    gt_names = []
    for nb in ground_truth["neighborhoods"]:
        try:
            g = shape(nb["geojson"])
            if not g.is_valid:
                g = make_valid(g)
            gt_polys.append(g)
            gt_names.append(nb["name"])
        except Exception:
            continue
    if not gt_polys:
        return {"district": district_name, "skipped": "no valid ground-truth polygons"}

    tree = STRtree(gt_polys)

    log(f"  Loading buildings for {district_name} (centroid spatial match)...")
    rows = psql_copy(
        f"SELECT b.id, b.neighborhood_id, n.name AS assigned_neighborhood, "
        f"b.geometry::text AS geometry, b.latitude, b.longitude "
        f"FROM buildings b JOIN neighborhoods n ON n.id=b.neighborhood_id "
        f"JOIN districts d ON d.id=b.district_id WHERE d.name='{district_name}'"
    )

    def norm(n):  # same normalization family as BuildingImportService.normalize()
        s = n.strip().lower()
        for sfx in (" mahallesi", " mah.", " mh."):
            if s.endswith(sfx):
                return s[: -len(sfx)].strip()
        return s

    correct = 0
    outside_all = 0
    remappable = []  # unique alternate match found
    ambiguous = []   # multiple candidate matches, or edge-of-tolerance
    invalid_geom = 0
    total = len(rows)

    for r in rows:
        issues = []
        geom = parse_geometry(r["geometry"], f"building:{r['id']}", issues)
        if geom is None:
            invalid_geom += 1
            continue
        # Primary point: the building's stored centroid (lat/lon), matching
        # exactly what BuildingImportService/fetch_district_buildings.py
        # used to decide neighborhood membership at import time.
        pt = Point(float(r["longitude"]), float(r["latitude"]))

        assigned_norm = norm(r["assigned_neighborhood"])
        candidate_idxs = tree.query(pt)
        matches = []
        for ci in candidate_idxs:
            poly = gt_polys[ci]
            if poly.contains(pt) or poly.buffer(SLIVER_TOL).contains(pt):
                matches.append(gt_names[ci])

        if not matches:
            outside_all += 1
            continue

        matched_norms = {norm(m) for m in matches}
        if assigned_norm in matched_norms:
            correct += 1
        elif len(matched_norms) == 1:
            remappable.append({
                "building_id": r["id"],
                "assigned": r["assigned_neighborhood"],
                "spatial_match": matches[0],
            })
        else:
            ambiguous.append({
                "building_id": r["id"],
                "assigned": r["assigned_neighborhood"],
                "spatial_candidates": matches,
            })

    return {
        "district": district_name,
        "total_buildings": total,
        "invalid_geometry": invalid_geom,
        "correct": correct,
        "outside_all_gt_neighborhoods": outside_all,
        "remappable_count": len(remappable),
        "remappable_sample": remappable[:25],
        "remappable_all": remappable,
        "ambiguous_count": len(ambiguous),
        "ambiguous_sample": ambiguous[:10],
    }


def audit_district_pair_overlaps(district_results: dict) -> list[dict]:
    log("=== Checking district-pair overlaps ===")
    names = list(district_results.keys())
    findings = []
    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            a, b = names[i], names[j]
            ga, gb = district_results[a].get("_shape"), district_results[b].get("_shape")
            if ga is None or gb is None:
                continue
            if not ga.intersects(gb):
                continue
            inter = ga.intersection(gb)
            if inter.is_empty:
                continue
            ratio_a = inter.area / ga.area if ga.area > 0 else 0
            ratio_b = inter.area / gb.area if gb.area > 0 else 0
            # a shared border line has ~zero area; only report real area overlap
            if max(ratio_a, ratio_b) > 0.01:
                findings.append({"a": a, "b": b, "overlap_ratio_a": ratio_a, "overlap_ratio_b": ratio_b})
    log(f"  {len(findings)} district pairs with >1% area overlap")
    return findings


def audit_neighborhood_vs_parent(district_name: str, ground_truth: dict, db_neighborhoods: list[dict]) -> list[dict]:
    district_geom = None
    if ground_truth:
        try:
            district_geom = shape(ground_truth["district_geojson"])
        except Exception:
            district_geom = None

    findings = []
    for nb in db_neighborhoods:
        issues = []
        geom = parse_geometry(nb["geojson_polygon"], f"neighborhood:{district_name}/{nb['name']}", issues)
        entry = {
            "district": district_name,
            "neighborhood": nb["name"],
            "neighborhood_id": nb["id"],
            "issues": [str(i) for i in issues],
            "is_sliver": is_sliver(geom) if geom else None,
        }
        if geom is not None:
            entry["area_deg2"] = geom.area
            entry["geometry_type"] = geom.geom_type
            if district_geom is not None:
                try:
                    inter = geom.intersection(district_geom)
                    entry["containment_ratio"] = (inter.area / geom.area) if geom.area > 0 else 0
                except Exception:
                    entry["containment_ratio"] = None
        findings.append(entry)
    return findings


def main():
    refresh = "--refresh-cache" in sys.argv
    districts_arg = None
    for a in sys.argv:
        if a.startswith("--districts="):
            districts_arg = a.split("=", 1)[1].split(",")
    district_names = districts_arg or DISTRICTS_EUROPEAN

    os.makedirs(CACHE_DIR, exist_ok=True)

    report = {"districts": district_names, "generated_at": time.strftime("%Y-%m-%dT%H:%M:%S")}

    district_results = audit_districts(district_names)
    report["district_geometry"] = {
        name: {k: v for k, v in r.items() if k != "_shape"} for name, r in district_results.items()
    }

    report["district_pair_overlaps"] = audit_district_pair_overlaps(district_results)

    nb_by_district = audit_neighborhoods(district_names)

    log("=== Fetching/loading OSM ground truth per district ===")
    ground_truths = {}
    for name in district_names:
        ground_truths[name] = load_or_fetch_osm_ground_truth(name, refresh)

    report["neighborhood_geometry"] = {}
    for name in district_names:
        report["neighborhood_geometry"][name] = audit_neighborhood_vs_parent(
            name, ground_truths.get(name), nb_by_district.get(name, [])
        )

    report["building_fk_mismatches"] = audit_building_fk_consistency(district_names)
    report["building_bounds_violations"] = audit_building_bounds_sanity(district_names)

    report["building_spatial_match"] = {}
    for name in district_names:
        report["building_spatial_match"][name] = audit_building_spatial_match(
            name, ground_truths.get(name), nb_by_district.get(name, [])
        )

    with open(REPORT_PATH, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2, default=str)
    log(f"\nFull report written to {REPORT_PATH}")

    # ---- console summary ----
    print("\n" + "=" * 70)
    print("SUMMARY")
    print("=" * 70)
    for name in district_names:
        dg = report["district_geometry"][name]
        nb_list = report["neighborhood_geometry"][name]
        slivers = [n for n in nb_list if n.get("is_sliver")]
        bad_containment = [n for n in nb_list if n.get("containment_ratio") is not None and n["containment_ratio"] < 0.5]
        bm = report["building_spatial_match"].get(name, {})
        print(f"\n--- {name} ---")
        print(f"  District geometry: {dg.get('geometry_type')} issues={dg.get('issues')}")
        print(f"  Neighborhoods: {len(nb_list)} total, {len(slivers)} sliver, {len(bad_containment)} <50% inside parent")
        if bm.get("skipped"):
            print(f"  Building spatial match: SKIPPED ({bm['skipped']})")
        else:
            print(f"  Building spatial match: total={bm.get('total_buildings')} correct={bm.get('correct')} "
                  f"remappable={bm.get('remappable_count')} ambiguous={bm.get('ambiguous_count')} "
                  f"outside_all={bm.get('outside_all_gt_neighborhoods')} invalid_geom={bm.get('invalid_geometry')}")

    print(f"\nDistrict pair overlaps (>1% area): {len(report['district_pair_overlaps'])}")
    for o in report["district_pair_overlaps"]:
        print(f"  {o['a']} <-> {o['b']}: {o['overlap_ratio_a']:.2%} / {o['overlap_ratio_b']:.2%}")
    print(f"\nBuilding FK mismatches (building.district_id != neighborhood's district_id): {len(report['building_fk_mismatches'])}")
    print(f"Buildings outside Istanbul bbox: {len(report['building_bounds_violations'])}")


if __name__ == "__main__":
    main()
