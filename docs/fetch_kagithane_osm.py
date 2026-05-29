#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Kağıthane mahalle sınırlarını OSM Overpass API'den çeker,
V43 Flyway migration SQL'i üretir.

V42'deki self-intersecting ring sorunu:
  relation_to_polygon() way'leri naif concatenation ile birleştiriyordu.
  OSM relation'larının birden fazla outer way üyesi olduğunda, way'lerin
  endpoint matching ile zincire eklenmesi gerekir; aksi hâlde ring
  self-intersecting olur ve Leaflet SVG evenodd fill'de üçgen/parça görünür.

Bu script:
  - assemble_rings(): way'leri endpoint matching + reversal ile zincirler
  - Doğru OSM relation #1765894 (Kâğıthane ilçesi, admin_level=6)
  - Türkiye mahalle admin_level = 8
  - Sadece mahalle UPDATE'leri üretir (V42 district verisi doğru, korunur)

Kullanım:
  python docs/fetch_kagithane_osm.py
  -> V43__kagithane_neighborhood_rings_fix.sql üretir
"""

import io
import json
import os
import sys
import time
import urllib.request
import urllib.parse

# Force UTF-8 stdout
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
OVERPASS_MIRRORS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
]

KAGITHANE_NEIGHBORHOODS = [
    "Çağlayan", "Çeliktepe", "Emniyet Evleri", "Gültepe", "Gürsel",
    "Hamidiye", "Harmantepe", "Hürriyet", "Mehmet Akif Ersoy",
    "Ortabayır", "Sultan Selim", "Şirintepe", "Talatpaşa",
    "Telsizler", "Yahya Kemal", "Yeşilce",
]

DISTRICT_UUID = "11111111-1111-1111-1111-000000000033"

OUTPUT_FILE = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "backend", "src", "main", "resources", "db", "migration",
    "V43__kagithane_neighborhood_rings_fix.sql"
)

CACHE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_kagithane_osm_cache.json")


# ── Overpass helpers ──────────────────────────────────────────────────────────

def overpass_query(query: str) -> dict:
    """Tries all mirrors in order; returns first successful response."""
    last_err = None
    for url in OVERPASS_MIRRORS:
        try:
            data = urllib.parse.urlencode({"data": query}).encode("utf-8")
            req = urllib.request.Request(
                url, data=data,
                headers={"Content-Type": "application/x-www-form-urlencoded",
                         "Accept-Charset": "utf-8",
                         "User-Agent": "afet-koordinasyon/1.0 (educational project)"}
            )
            with urllib.request.urlopen(req, timeout=90) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except Exception as e:
            print(f"  Mirror {url} failed: {e}", file=sys.stderr)
            last_err = e
            time.sleep(2)
    raise RuntimeError(f"All Overpass mirrors failed. Last error: {last_err}")


def load_cache() -> dict:
    if os.path.exists(CACHE_FILE):
        try:
            with open(CACHE_FILE, encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {}


def save_cache(cache: dict) -> None:
    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False)


def fetch_with_cache(key: str, query: str, cache: dict) -> dict:
    if key in cache:
        print(f"  [cache] {key}", file=sys.stderr)
        return cache[key]
    result = overpass_query(query)
    cache[key] = result
    save_cache(cache)
    time.sleep(2)
    return result


def fetch_neighborhoods_at_level(level: int, cache: dict) -> dict:
    # admin_level=8 is correct for Turkish mahalle (neighborhood)
    query = f"""
[out:json][timeout:90];
(
  relation["admin_level"="{level}"]["boundary"="administrative"]
          (41.045,28.895,41.135,29.015);
);
out geom;
"""
    print(f"Fetching admin_level={level} in Kağıthane bbox...", file=sys.stderr)
    return fetch_with_cache(f"nh_level_{level}", query, cache)


# ── Ring assembly (endpoint chaining) ─────────────────────────────────────────

def _pt(p: dict) -> tuple:
    return (round(p["lon"], 6), round(p["lat"], 6))


def _eq(a: tuple, b: tuple, eps: float = 1e-6) -> bool:
    return abs(a[0] - b[0]) < eps and abs(a[1] - b[1]) < eps


def assemble_rings(element: dict) -> list:
    """
    OSM relation'ının outer way üyelerini endpoint matching ile
    düzgün kapalı ring(ler)e birleştirir.

    Naif concatenation (eski kod):
        for way in outer_ways: coords += way.geometry
        → Way'ler sırasız geldiğinde self-intersecting ring

    Bu implementasyon:
        1. Tüm outer way'leri (lon,lat) tuple listelerine dönüştür
        2. İlk way'i ring başlangıcı olarak al
        3. Ring'in son noktasına başlangıç/bitiş noktası eşleşen
           bir sonraki way'i bul (gerekirse ters çevirerek)
        4. Ring kapanana veya way kalmayına dek devam et
        5. Kalan way'ler varsa (MultiPolygon durumu) ayrı ring olarak
           döndür; caller'ın MultiPolygon kararı vermesi gerekir

    Returns:
        list of rings, her ring [[lon, lat], ...] (kapalı, first == last)
    """
    members = element.get("members", [])
    outer_ways = [m for m in members
                  if m.get("role") == "outer" and m.get("type") == "way"]
    if not outer_ways:
        return []

    # Way geometrilerini tuple listelerine dönüştür (boş way'leri atla)
    way_seqs = []
    for way in outer_ways:
        geom = way.get("geometry", [])
        if len(geom) >= 2:
            way_seqs.append([_pt(p) for p in geom])

    if not way_seqs:
        return []

    # Tek way → trivial ring
    if len(way_seqs) == 1:
        ring = list(way_seqs[0])
        if not _eq(ring[0], ring[-1]):
            ring.append(ring[0])
        return [ring]

    rings = []
    remaining = list(way_seqs)

    while remaining:
        ring = list(remaining.pop(0))

        # Chain: ring'in son noktasıyla başlayan/biten bir way ara
        changed = True
        while changed and remaining:
            changed = False
            cur = ring[-1]
            for i, way in enumerate(remaining):
                if _eq(way[0], cur):
                    # Forward match: way başlangıcı ring sonuyla eşleşiyor
                    ring.extend(way[1:])
                    remaining.pop(i)
                    changed = True
                    break
                elif _eq(way[-1], cur):
                    # Reverse match: way sonu ring sonuyla eşleşiyor
                    ring.extend(list(reversed(way[:-1])))
                    remaining.pop(i)
                    changed = True
                    break

            # Ring kapandıysa dur
            if len(ring) > 3 and _eq(ring[0], ring[-1]):
                break

        # Ring'i kapat
        if not _eq(ring[0], ring[-1]):
            ring.append(ring[0])

        rings.append([[p[0], p[1]] for p in ring])

    return rings


# ── Simplification (Ramer-Douglas-Peucker) ────────────────────────────────────

def rdp(points: list, eps: float) -> list:
    if len(points) < 3:
        return points
    start, end = points[0], points[-1]
    ax, ay = start
    bx, by = end
    dx, dy = bx - ax, by - ay
    denom = dx * dx + dy * dy
    dmax, idx = 0.0, 0
    for i in range(1, len(points) - 1):
        px, py = points[i]
        if denom == 0:
            d = ((px - ax) ** 2 + (py - ay) ** 2) ** 0.5
        else:
            t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / denom))
            d = ((px - (ax + t * dx)) ** 2 + (py - (ay + t * dy)) ** 2) ** 0.5
        if d > dmax:
            dmax, idx = d, i
    if dmax > eps:
        return rdp(points[:idx + 1], eps)[:-1] + rdp(points[idx:], eps)
    return [start, end]


def simplify(coords: list, tolerance: float = 0.0002) -> list:
    if len(coords) <= 4:
        return coords
    s = rdp(coords[:-1], tolerance)
    if s[0] != s[-1]:
        s.append(s[0])
    return s


# ── Spatial filter ────────────────────────────────────────────────────────────

def inside_kagithane(coords: list) -> bool:
    """Ring koordinatlarının Kağıthane genel bbox'u içinde olup olmadığını kontrol et."""
    lons = [c[0] for c in coords]
    lats = [c[1] for c in coords]
    return (28.88 <= min(lons) and max(lons) <= 29.02 and
            41.04 <= min(lats) and max(lats) <= 41.14)


# ── Name matching ─────────────────────────────────────────────────────────────

def normalize(s: str) -> str:
    return (s.lower()
             .replace("ı", "i").replace("İ", "i")
             .replace("ğ", "g").replace("Ğ", "g")
             .replace("ü", "u").replace("Ü", "u")
             .replace("ş", "s").replace("Ş", "s")
             .replace("ö", "o").replace("Ö", "o")
             .replace("ç", "c").replace("Ç", "c")
             .replace(" mahallesi", "").replace(" mahalle", "")
             .replace(" mah.", "").replace(" mh.", "")
             .replace(" ", "").replace("-", "").strip())


def match_name(osm_name: str, db_names: list) -> str | None:
    on = normalize(osm_name)
    for db in db_names:
        dn = normalize(db)
        if on == dn or on.startswith(dn) or dn.startswith(on):
            return db
    return None


# ── SQL helpers ───────────────────────────────────────────────────────────────

def coords_to_json_str(coords: list) -> str:
    pts = ",".join(f"[{c[0]},{c[1]}]" for c in coords)
    return f'[{pts}]'


def validate_ring(coords: list, name: str) -> bool:
    """Ring kalitesini kontrol et; sorun varsa stderr'e yaz."""
    ok = True
    if len(coords) < 4:
        print(f"  WARN {name}: sadece {len(coords)} nokta", file=sys.stderr)
        ok = False
    if coords[0] != coords[-1]:
        print(f"  WARN {name}: ring kapalı değil", file=sys.stderr)
        ok = False
    # Ardışık tekrar kontrol
    dups = sum(1 for i in range(len(coords) - 1) if coords[i] == coords[i + 1])
    if dups:
        print(f"  WARN {name}: {dups} ardışık tekrar nokta", file=sys.stderr)
    # Max kenar uzunluğu (km) — self-intersecting ring genellikle büyük sıçramalar içerir
    max_edge = 0.0
    for i in range(len(coords) - 1):
        dlon = (coords[i + 1][0] - coords[i][0]) * 75.0   # ~75 km/degree lon at lat 41
        dlat = (coords[i + 1][1] - coords[i][1]) * 111.0  # ~111 km/degree lat
        edge = (dlon ** 2 + dlat ** 2) ** 0.5
        if edge > max_edge:
            max_edge = edge
    if max_edge > 2.0:
        print(f"  WARN {name}: max kenar {max_edge:.2f} km > 2 km eşiği "
              f"(self-intersecting olabilir)", file=sys.stderr)
        ok = False
    return ok


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    cache = load_cache()

    lines = []
    lines.append("-- =====================================================================")
    lines.append("-- V43: Kağıthane mahalle ring assembly düzeltmesi")
    lines.append("--")
    lines.append("-- V42'deki sorun: relation_to_polygon() OSM way'lerini naif")
    lines.append("-- concatenation ile birleştiriyordu → self-intersecting ring →")
    lines.append("-- Leaflet SVG evenodd fill'de üçgen/parça render görünümü.")
    lines.append("--")
    lines.append("-- Düzeltme: assemble_rings() way'leri endpoint matching + reversal")
    lines.append("-- ile zincirler → düzgün kapalı, non-self-intersecting ring.")
    lines.append("-- Kaynak: OSM admin_level=8, Kağıthane (içinde district #1765894)")
    lines.append("-- =====================================================================")
    lines.append("")
    lines.append("-- NOT: V42 district polygon'u doğruydu, bu migration sadece")
    lines.append("-- neighborhood polygon'larını yeniden yazar.")
    lines.append("")

    matched_names = set()

    # admin_level=8 → Türkiye mahalleleri (neighborhood)
    # Fallback olarak 9 ve 10 de denenir
    for level in [8, 9, 10]:
        try:
            data = fetch_neighborhoods_at_level(level, cache)
            elements = data.get("elements", [])
            print(f"  admin_level={level}: {len(elements)} relation bulundu", file=sys.stderr)

            for el in elements:
                if el.get("type") != "relation":
                    continue
                tags = el.get("tags", {})
                osm_name = tags.get("name", "")
                if not osm_name:
                    continue

                db_name = match_name(osm_name, KAGITHANE_NEIGHBORHOODS)
                if not db_name or db_name in matched_names:
                    continue

                # assemble_rings: endpoint matching ile way zinciri
                rings = assemble_rings(el)
                if not rings:
                    print(f"  SKIP {db_name}: ring assembly başarısız", file=sys.stderr)
                    continue

                # İlk ring'i kullan (MultiPolygon için en büyüğünü seç)
                if len(rings) > 1:
                    rings.sort(key=len, reverse=True)
                    print(f"  INFO {db_name}: {len(rings)} ring, en büyük kullanılıyor", file=sys.stderr)
                coords = rings[0]

                if not inside_kagithane(coords):
                    print(f"  SKIP {db_name}: Kağıthane bbox dışında", file=sys.stderr)
                    continue

                simplified = simplify(coords, tolerance=0.00015)

                if not validate_ring(simplified, db_name):
                    print(f"  SKIP {db_name}: ring doğrulaması başarısız", file=sys.stderr)
                    continue

                coord_json = coords_to_json_str(simplified)

                lines.append(f"-- {db_name} — OSM: '{osm_name}' (admin_level={level}, "
                             f"{len(simplified)} vertices, ring assembly OK)")
                lines.append("UPDATE neighborhoods")
                lines.append(f"   SET geojson_polygon = "
                             f"'{{\"type\":\"Polygon\",\"coordinates\":[{coord_json}]}}',")
                lines.append("       updated_at = now()")
                lines.append(f" WHERE name = '{db_name}'")
                lines.append(f"   AND district_id = '{DISTRICT_UUID}';")
                lines.append("")
                matched_names.add(db_name)
                print(f"  OK  {db_name}: {len(simplified)} v, "
                      f"admin_level={level}", file=sys.stderr)

        except Exception as e:
            print(f"  admin_level={level} hatası: {e}", file=sys.stderr)

    # Eşleştirilemeyen mahalleler → NULL (yanlış polygon göstermekten iyidir)
    remaining = [n for n in KAGITHANE_NEIGHBORHOODS if n not in matched_names]
    if remaining:
        lines.append("-- Eşleştirilemeyen mahalleler → NULL")
        lines.append("-- (Yanlış polygon render etmekten daha iyi)")
        for name in remaining:
            lines.append(f"-- NULL: {name}")
            lines.append("UPDATE neighborhoods")
            lines.append("   SET geojson_polygon = NULL, updated_at = now()")
            lines.append(f" WHERE name = '{name}'")
            lines.append(f"   AND district_id = '{DISTRICT_UUID}';")
            lines.append("")
        print(f"  NULL olarak işaretlenen: {remaining}", file=sys.stderr)

    lines.append("-- Risk skoru NULL → 0")
    lines.append("UPDATE neighborhoods")
    lines.append("   SET risk_score = COALESCE(risk_score, 0), updated_at = now()")
    lines.append(f" WHERE district_id = '{DISTRICT_UUID}' AND risk_score IS NULL;")

    sql = "\n".join(lines)
    out_path = os.path.normpath(OUTPUT_FILE)

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(sql)

    print(f"\nOutput: {out_path}", file=sys.stderr)
    print(f"Matched {len(matched_names)}/{len(KAGITHANE_NEIGHBORHOODS)} neighborhoods",
          file=sys.stderr)
    if remaining:
        print(f"Unmatched: {remaining}", file=sys.stderr)


if __name__ == "__main__":
    main()
