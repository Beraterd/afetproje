export const MAP_TILE_URL =
    import.meta.env.VITE_MAP_TILE_URL || 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';

export const MAP_TILE_ATTRIBUTION =
    import.meta.env.VITE_MAP_ATTRIBUTION ||
    '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

// MapLibre does not expand Leaflet's subdomain and retina placeholders.
export const MAP_RASTER_TILE_URLS = MAP_TILE_URL.includes('{s}')
    ? ['a', 'b', 'c'].map((subdomain) => MAP_TILE_URL.replace('{s}', subdomain).replace('{r}', ''))
    : [MAP_TILE_URL.replace('{r}', '')];
