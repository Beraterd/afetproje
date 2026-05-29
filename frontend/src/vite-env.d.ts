/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string;
    readonly VITE_MAP_TILE_URL: string;
    readonly VITE_MAP_ATTRIBUTION: string;
    readonly VITE_APP_TITLE: string;
    readonly VITE_ENABLE_DEVTOOLS: string;
    readonly VITE_SESSION_STORAGE_KEY: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
