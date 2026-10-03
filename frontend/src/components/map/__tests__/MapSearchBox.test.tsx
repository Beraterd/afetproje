import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MapSearchBox } from '../MapSearchBox';
import { StreetSearchResultResponse } from '@/types/street';
import { BuildingSearchResultResponse } from '@/types/building';

vi.mock('@/api/streets.api', () => ({ searchStreets: vi.fn() }));
vi.mock('@/api/buildings.api', () => ({ searchBuildings: vi.fn() }));

import { searchStreets } from '@/api/streets.api';
import { searchBuildings } from '@/api/buildings.api';

const DISTRICT_ID = 'district-1';
const NEIGHBORHOOD_ID = 'nb-1';

const STREET: StreetSearchResultResponse = {
    id: 'street-1',
    name: 'Selvi Boylum Sokak',
    districtId: DISTRICT_ID,
    districtName: 'Pendik',
    neighborhoodId: NEIGHBORHOOD_ID,
    neighborhoodName: 'Kurtköy',
    roadType: 'residential',
    bounds: { minLon: 29.1, minLat: 40.9, maxLon: 29.11, maxLat: 40.91 },
    center: { lon: 29.105, lat: 40.905 },
};

const BUILDING: BuildingSearchResultResponse = {
    id: 'building-1',
    displayAddress: 'Selvi Sokak 5, Kurtköy Mah., Pendik/İstanbul',
    streetName: 'Selvi Sokak',
    buildingNumber: '5',
    neighborhoodName: 'Kurtköy',
    latitude: 40.906,
    longitude: 29.106,
};

function renderBox(overrides: Partial<React.ComponentProps<typeof MapSearchBox>> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const onSelectStreet = vi.fn();
    const onSelectBuilding = vi.fn();
    const utils = render(
        <QueryClientProvider client={queryClient}>
            <MapSearchBox
                districtId={DISTRICT_ID}
                districtName="Pendik"
                neighborhoodId={NEIGHBORHOOD_ID}
                onSelectStreet={onSelectStreet}
                onSelectBuilding={onSelectBuilding}
                {...overrides}
            />
        </QueryClientProvider>,
    );
    return { ...utils, onSelectStreet, onSelectBuilding };
}

beforeEach(() => {
    vi.clearAllMocks();
    (searchStreets as ReturnType<typeof vi.fn>).mockResolvedValue([STREET]);
    (searchBuildings as ReturnType<typeof vi.fn>).mockResolvedValue([BUILDING]);
});

describe('MapSearchBox — arama ve debounce', () => {
    it('2 karakterden kısa sorguda hiçbir API çağrılmaz', async () => {
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 's');

        await new Promise((r) => setTimeout(r, 400));
        expect(searchStreets).not.toHaveBeenCalled();
        expect(searchBuildings).not.toHaveBeenCalled();
    });

    it('debounce sonrası sokak araması doğru districtId+neighborhoodId ile TEK sefer çağrılır', async () => {
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');

        await waitFor(() => expect(searchStreets).toHaveBeenCalled(), { timeout: 1000 });
        expect(searchStreets).toHaveBeenCalledTimes(1);
        expect(searchStreets).toHaveBeenCalledWith('selvi', DISTRICT_ID, NEIGHBORHOOD_ID, undefined, expect.anything());
    });

    it('bina araması yalnızca 3+ karakterde tetiklenir (sokaktan farklı eşik)', async () => {
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'se');

        await waitFor(() => expect(searchStreets).toHaveBeenCalled());
        expect(searchBuildings).not.toHaveBeenCalled();
    });

    it('mahalle seçili değilse sokak araması hiç çağrılmaz, bina araması (ilçe-scoped) çalışmaya devam eder', async () => {
        renderBox({ neighborhoodId: null });
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');

        await waitFor(() => expect(searchBuildings).toHaveBeenCalled());
        expect(searchStreets).not.toHaveBeenCalled();
        expect(await screen.findByText('Sokak aramak için önce bir mahalle seçin.')).toBeInTheDocument();
    });
});

describe('MapSearchBox — sonuç listesi ve kategoriler', () => {
    it('sonuçlar "Sokaklar" ve "Binalar / Adresler" başlıkları altında kategorize gösterilir', async () => {
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');

        expect(await screen.findByText('Sokaklar')).toBeInTheDocument();
        expect(await screen.findByText('Binalar / Adresler')).toBeInTheDocument();
        expect(screen.getByText('Selvi Boylum Sokak')).toBeInTheDocument();
        expect(screen.getByText('Selvi Sokak 5, Kurtköy Mah., Pendik/İstanbul')).toBeInTheDocument();
    });

    it('sonuç yoksa genel "bulunamadı" mesajı gösterilir (adres-özel dil kullanılmaz)', async () => {
        (searchStreets as ReturnType<typeof vi.fn>).mockResolvedValue([]);
        (searchBuildings as ReturnType<typeof vi.fn>).mockResolvedValue([]);
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'zzzznomatch');

        expect(await screen.findByText('Bu mahallede eşleşen sokak veya adres bulunamadı.')).toBeInTheDocument();
    });
});

describe('MapSearchBox — seçim (tıklama/klavye)', () => {
    it('bir sokak sonucuna tıklamak onSelectStreet çağırır ve input temizlenir', async () => {
        const { onSelectStreet, onSelectBuilding } = renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');

        const option = await screen.findByText('Selvi Boylum Sokak');
        await user.click(option);

        expect(onSelectStreet).toHaveBeenCalledWith(STREET);
        expect(onSelectBuilding).not.toHaveBeenCalled();
        expect(screen.getByRole('combobox')).toHaveValue('');
    });

    it('bir bina sonucuna tıklamak onSelectBuilding çağırır (mevcut davranış korunur)', async () => {
        const { onSelectBuilding } = renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');

        const option = await screen.findByText('Selvi Sokak 5, Kurtköy Mah., Pendik/İstanbul');
        await user.click(option);

        expect(onSelectBuilding).toHaveBeenCalledWith(BUILDING);
    });

    it('vurgu yokken Enter — ilk sonucu (sokak, kategoride önce listelenir) seçer', async () => {
        const { onSelectStreet } = renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');
        await screen.findByText('Selvi Boylum Sokak');

        await user.keyboard('{Enter}');

        expect(onSelectStreet).toHaveBeenCalledWith(STREET);
    });

    it('ArrowDown ile ikinci sonuca (bina) inip Enter — o sonucu seçer', async () => {
        const { onSelectBuilding, onSelectStreet } = renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');
        await screen.findByText('Selvi Boylum Sokak');

        await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');

        expect(onSelectBuilding).toHaveBeenCalledWith(BUILDING);
        expect(onSelectStreet).not.toHaveBeenCalled();
    });

    it('ArrowDown sonrası ArrowUp önceki sonuca geri döner', async () => {
        const { onSelectStreet } = renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');
        await screen.findByText('Selvi Boylum Sokak');

        await user.keyboard('{ArrowDown}{ArrowDown}{ArrowUp}{Enter}');

        expect(onSelectStreet).toHaveBeenCalledWith(STREET);
    });

    it('boş sorguda Enter hiçbir şey yapmaz', async () => {
        const { onSelectStreet, onSelectBuilding } = renderBox();
        const user = userEvent.setup();
        await user.click(screen.getByRole('combobox'));
        await user.keyboard('{Enter}');

        expect(onSelectStreet).not.toHaveBeenCalled();
        expect(onSelectBuilding).not.toHaveBeenCalled();
    });

    it('Escape dropdown\'ı kapatır', async () => {
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');
        await screen.findByText('Selvi Boylum Sokak');

        await user.keyboard('{Escape}');

        expect(screen.queryByText('Selvi Boylum Sokak')).not.toBeInTheDocument();
    });

    it('Escape sonrası tekrar yazmak dropdown\'ı yeniden açar', async () => {
        renderBox();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'selvi');
        await screen.findByText('Selvi Boylum Sokak');
        await user.keyboard('{Escape}');
        expect(screen.queryByText('Selvi Boylum Sokak')).not.toBeInTheDocument();

        await user.type(screen.getByRole('combobox'), 'x');

        expect(await screen.findByText('Selvi Boylum Sokak')).toBeInTheDocument();
    });
});
