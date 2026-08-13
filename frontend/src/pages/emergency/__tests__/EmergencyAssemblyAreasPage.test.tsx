import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { EmergencyAssemblyAreasPage } from '@/pages/emergency/EmergencyAssemblyAreasPage';

vi.mock('@/api/emergencyContacts.api', () => ({
    getMyAssemblyAreas: vi.fn(),
}));

import { getMyAssemblyAreas } from '@/api/emergencyContacts.api';

const areas = [
    { id: 'far',  name: 'Uzak Toplanma Alanı',  address: 'Uzak adres',  latitude: 41.10, longitude: 29.10, googleMapsUrl: null },
    { id: 'near', name: 'Yakın Toplanma Alanı',  address: 'Yakın adres', latitude: 41.001, longitude: 29.001, googleMapsUrl: null },
    { id: 'none', name: 'Konumsuz Alan',         address: 'Adres bilgisi', latitude: null, longitude: null, googleMapsUrl: null },
];

// Kullanıcı konumu — "near" alana çok yakın, "far" alana uzak.
const USER_LAT = 41.0;
const USER_LNG = 29.0;

function mockGeolocationSuccess() {
    (global as any).navigator.geolocation = {
        getCurrentPosition: (success: PositionCallback) => {
            success({
                coords: {
                    latitude: USER_LAT, longitude: USER_LNG, accuracy: 15,
                    altitude: null, altitudeAccuracy: null, heading: null, speed: null,
                },
                timestamp: Date.now(),
            } as GeolocationPosition);
        },
        watchPosition: vi.fn(),
        clearWatch: vi.fn(),
    };
}

function mockGeolocationDenied() {
    (global as any).navigator.geolocation = {
        getCurrentPosition: (_success: PositionCallback, error: PositionErrorCallback) => {
            error({ code: 1, message: 'User denied Geolocation', PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3 } as GeolocationPositionError);
        },
        watchPosition: vi.fn(),
        clearWatch: vi.fn(),
    };
}

beforeEach(() => {
    vi.clearAllMocks();
    (getMyAssemblyAreas as any).mockResolvedValue(areas);
});

afterEach(() => {
    delete (global as any).navigator.geolocation;
});

describe('EmergencyAssemblyAreasPage — geolocation granted', () => {
    it('calculates and displays distances once geolocation succeeds', async () => {
        mockGeolocationSuccess();
        render(<EmergencyAssemblyAreasPage />);

        // "near" alanı ~139m, "far" alanı ~13.9km uzaklıkta olmalı
        await waitFor(() => {
            expect(screen.getByText(/139 m/)).toBeInTheDocument();
        });
        expect(screen.getByText(/13\.9 km/)).toBeInTheDocument();
    });

    it('marks the closest area with the "En Yakın" badge', async () => {
        mockGeolocationSuccess();
        render(<EmergencyAssemblyAreasPage />);

        await waitFor(() => expect(screen.getByText('En Yakın')).toBeInTheDocument());
        const nearHeading = screen.getByText('Yakın Toplanma Alanı');
        const nearCard = nearHeading.closest('div.bg-white')!;
        expect(nearCard).toContainElement(screen.getByText('En Yakın'));
    });

    it('sorts the list by distance ascending (nearest first)', async () => {
        mockGeolocationSuccess();
        render(<EmergencyAssemblyAreasPage />);

        await waitFor(() => expect(screen.getByText('En Yakın')).toBeInTheDocument());
        const headings = screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent);
        // "near" en yakın, sonra "far", konumsuz olan ("none") en sona düşer.
        expect(headings).toEqual(['Yakın Toplanma Alanı', 'Uzak Toplanma Alanı', 'Konumsuz Alan']);
    });

    it('shows an estimated walking time alongside the distance', async () => {
        mockGeolocationSuccess();
        render(<EmergencyAssemblyAreasPage />);

        await waitFor(() => expect(screen.getByText(/Tahmini 2 dk yürüme/)).toBeInTheDocument());
        expect(screen.getByText(/Tahmini 174 dk yürüme/)).toBeInTheDocument();
    });
});

describe('EmergencyAssemblyAreasPage — geolocation denied', () => {
    it('does not break the screen — list still renders with addresses instead of distance', async () => {
        mockGeolocationDenied();
        render(<EmergencyAssemblyAreasPage />);

        await screen.findByText('Uzak Toplanma Alanı');
        expect(screen.getByText('Yakın Toplanma Alanı')).toBeInTheDocument();
        expect(screen.getByText('Uzak adres')).toBeInTheDocument();
        expect(screen.queryByText('En Yakın')).not.toBeInTheDocument();
        // Pasif izin mesajı gösterilir, agresif bir şekilde otomatik tekrar denenmez.
        expect(await screen.findByText(/Konum izni verirseniz/)).toBeInTheDocument();
    });
});
