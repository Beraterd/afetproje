// vite.config.ts
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
import path from 'path';

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    const apiTarget = env.VITE_API_BASE_URL || 'http://localhost:8080';

    return {
        plugins: [
            react(),
            VitePWA({
                registerType: 'autoUpdate',
                includeAssets: ['icons/icon.svg'],
                manifest: {
                    name: 'Afet Koordinasyon Sistemi',
                    short_name: 'AfetKoord',
                    description: 'Afet anında offline destekli koordinasyon sistemi',
                    theme_color: '#1d4ed8',
                    background_color: '#f9fafb',
                    display: 'standalone',
                    orientation: 'portrait',
                    start_url: '/',
                    scope: '/',
                    icons: [
                        {
                            src: '/icons/icon.svg',
                            sizes: '192x192',
                            type: 'image/svg+xml',
                            purpose: 'any',
                        },
                        {
                            src: '/icons/icon.svg',
                            sizes: '512x512',
                            type: 'image/svg+xml',
                            purpose: 'any maskable',
                        },
                    ],
                },
                workbox: {
                    // Precache all built assets
                    globPatterns: ['**/*.{js,css,html,ico,svg,woff,woff2}'],
                    // SPA fallback — serve index.html for all navigation except /api
                    navigateFallback: '/index.html',
                    navigateFallbackAllowlist: [/^(?!\/api).*/],
                    runtimeCaching: [
                        {
                            // OpenStreetMap tiles — cache-first, 7-day TTL, 500 tiles max
                            urlPattern: /^https:\/\/[abc]\.tile\.openstreetmap\.org\//,
                            handler: 'CacheFirst',
                            options: {
                                cacheName: 'osm-tiles',
                                expiration: {
                                    maxEntries: 500,
                                    maxAgeSeconds: 60 * 60 * 24 * 7,
                                },
                                cacheableResponse: { statuses: [0, 200] },
                            },
                        },
                    ],
                },
                // Disable in dev to avoid proxy conflicts; test with `npm run build && npm run preview`
                devOptions: {
                    enabled: false,
                },
            }),
        ],
        resolve: {
            alias: {
                '@': path.resolve(__dirname, 'src'),
            },
        },
        server: {
            port: 5173,
            proxy: {
                '/api': {
                    target: apiTarget,
                    changeOrigin: true,
                    secure: false,
                },
            },
        },
    };
});
