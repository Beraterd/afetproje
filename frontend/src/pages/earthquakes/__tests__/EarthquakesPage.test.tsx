import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';
import { ToastProvider } from '@/components/shared/ToastProvider';
import type { UserSummaryResponse } from '@/types';

vi.mock('@/api/earthquakes.api', () => ({
    getEarthquakes: vi.fn(),
    getEarthquakeProvinces: vi.fn(),
    getEarthquakeDistricts: vi.fn(),
    getEarthquakeDebugStatus: vi.fn(),
    syncEarthquakes: vi.fn(),
    syncKandilliEarthquakes: vi.fn(),
}));

// Gerçek Leaflet haritasını jsdom'da mount etmek yerine, EarthquakesPage'in haritaya
// GEÇTİĞİ nokta kümesini doğrulamak için sahte bir harita kullanılır (item 31).
vi.mock('@/components/earthquakes/EarthquakeMap', () => ({
    EarthquakeMap: ({ points }: any) => (
        <div data-testid="earthquake-map-stub">{points.length} nokta</div>
    ),
}));

import {
    getEarthquakes,
    getEarthquakeProvinces,
    getEarthquakeDistricts,
    getEarthquakeDebugStatus,
} from '@/api/earthquakes.api';
import { EarthquakesPage } from '@/pages/earthquakes/EarthquakesPage';
import type { EarthquakeEventResponse } from '@/types';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role };
}

function eq(overrides: Partial<EarthquakeEventResponse>): EarthquakeEventResponse {
    return {
        id: 'eq-' + Math.random(),
        externalId: 'ext-1',
        eventTime: '2026-08-08T14:00:00Z',
        latitude: 41.0,
        longitude: 29.0,
        depth: 7.5,
        magnitude: 4.2,
        magnitudeType: 'ML',
        location: 'Kartal (İstanbul)',
        province: 'İstanbul',
        district: 'Kartal',
        source: 'AFAD',
        riskLevel: 'MEDIUM',
        createdAt: '2026-08-08T14:00:05Z',
        ...overrides,
    };
}

function renderPage(initialEntries: string[] = ['/earthquakes']) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={initialEntries}>
                <ToastProvider>
                    <EarthquakesPage />
                </ToastProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: makeUser('VOLUNTEER'), accessToken: 't' });
    (getEarthquakes as any).mockResolvedValue({
        content: [eq({})], totalElements: 1, totalPages: 1, size: 20, number: 0, last: true, first: true, empty: false,
    });
    (getEarthquakeProvinces as any).mockResolvedValue(['Ankara', 'İstanbul', 'İzmir']);
    (getEarthquakeDistricts as any).mockResolvedValue([]);
    (getEarthquakeDebugStatus as any).mockResolvedValue({});
});

describe('EarthquakesPage — kaynak filtresi', () => {
    it('Tüm Kaynaklar / AFAD / Kandilli seçenekleri render edilir', async () => {
        renderPage();
        await waitFor(() => expect(getEarthquakes).toHaveBeenCalled());

        const select = screen.getByLabelText('Kaynak') as HTMLSelectElement;
        const options = within(select).getAllByRole('option').map((o) => o.textContent);
        expect(options).toEqual(['Tüm Kaynaklar', 'AFAD', 'Kandilli']);
    });

    it('kaynak seçilince API çağrısına source query param olarak gider', async () => {
        const userEv = userEvent.setup();
        renderPage();
        await waitFor(() => expect(getEarthquakes).toHaveBeenCalled());

        await userEv.selectOptions(screen.getByLabelText('Kaynak'), 'KANDILLI');

        await waitFor(() =>
            expect(getEarthquakes).toHaveBeenCalledWith(expect.objectContaining({ source: 'KANDILLI' })));
    });
});

describe('EarthquakesPage — il/ilçe filtresi', () => {
    it('il filtresi render edilir', async () => {
        renderPage();
        await waitFor(() => expect(getEarthquakeProvinces).toHaveBeenCalled());
        expect(screen.getByLabelText('İl')).toBeInTheDocument();
    });

    it('il seçilmeden ilçe dropdown disabled olur', async () => {
        renderPage();
        await waitFor(() => expect(getEarthquakes).toHaveBeenCalled());
        expect(screen.getByLabelText('İlçe')).toBeDisabled();
    });

    it('İstanbul seçilince İstanbul ilçeleri getEarthquakeDistricts ile çekilir ve dropdown aktifleşir', async () => {
        (getEarthquakeDistricts as any).mockResolvedValue(['Kartal', 'Kadıköy', 'Pendik']);
        const userEv = userEvent.setup();
        renderPage();
        await screen.findByRole('option', { name: 'İstanbul' });

        await userEv.selectOptions(screen.getByLabelText('İl'), 'İstanbul');

        await waitFor(() => expect(getEarthquakeDistricts).toHaveBeenCalledWith('İstanbul'));
        const districtSelect = await screen.findByLabelText('İlçe');
        expect(districtSelect).not.toBeDisabled();
        await waitFor(() => {
            const options = within(districtSelect).getAllByRole('option').map((o) => o.textContent);
            expect(options).toEqual(['Tüm İlçeler', 'Kartal', 'Kadıköy', 'Pendik']);
        });
    });

    it('il değiştirildiğinde önceki ilçe seçimi state\'te kalmaz', async () => {
        (getEarthquakeDistricts as any).mockResolvedValue(['Kartal']);
        const userEv = userEvent.setup();
        renderPage(['/earthquakes?province=%C4%B0stanbul&district=Kartal']);

        await waitFor(() => expect(screen.getByLabelText('İlçe')).toHaveValue('Kartal'));

        await userEv.selectOptions(screen.getByLabelText('İl'), 'Ankara');

        await waitFor(() => expect(screen.getByLabelText('İlçe')).toHaveValue(''));
    });

    it('province query param API request\'e gider', async () => {
        const userEv = userEvent.setup();
        renderPage();
        await screen.findByRole('option', { name: 'İzmir' });

        await userEv.selectOptions(screen.getByLabelText('İl'), 'İzmir');

        await waitFor(() =>
            expect(getEarthquakes).toHaveBeenCalledWith(expect.objectContaining({ province: 'İzmir' })));
    });

    it('district query param API request\'e gider', async () => {
        (getEarthquakeDistricts as any).mockResolvedValue(['Kartal']);
        const userEv = userEvent.setup();
        renderPage(['/earthquakes?province=%C4%B0stanbul']);
        await screen.findByRole('option', { name: 'Kartal' });

        await userEv.selectOptions(screen.getByLabelText('İlçe'), 'Kartal');

        await waitFor(() =>
            expect(getEarthquakes).toHaveBeenCalledWith(expect.objectContaining({ province: 'İstanbul', district: 'Kartal' })));
    });
});

describe('EarthquakesPage — URL senkronizasyonu', () => {
    it('filtreler URL search params ile senkronize olur (deep-link)', async () => {
        renderPage(['/earthquakes?source=KANDILLI&province=%C4%B0stanbul']);

        await waitFor(() =>
            expect(getEarthquakes).toHaveBeenCalledWith(
                expect.objectContaining({ source: 'KANDILLI', province: 'İstanbul' })));
        await screen.findByRole('option', { name: 'İstanbul' });
        expect(screen.getByLabelText('Kaynak')).toHaveValue('KANDILLI');
        expect(screen.getByLabelText('İl')).toHaveValue('İstanbul');
    });
});

describe('EarthquakesPage — kaynak badge', () => {
    it('Kandilli kaydı Kandilli badge gösterir', async () => {
        (getEarthquakes as any).mockResolvedValue({
            content: [eq({ source: 'KANDILLI' })], totalElements: 1, totalPages: 1, size: 20, number: 0, last: true, first: true, empty: false,
        });
        renderPage();
        expect(await screen.findByText('Kandilli')).toBeInTheDocument();
    });

    it('AFAD kaydı AFAD badge gösterir', async () => {
        renderPage();
        expect(await screen.findByText('AFAD')).toBeInTheDocument();
    });
});

describe('EarthquakesPage — boş sonuç ve provider uyarısı', () => {
    it('boş sonuç durumunda anlaşılır bir mesaj gösterir, sayfa çökmez', async () => {
        (getEarthquakes as any).mockResolvedValue({
            content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, last: true, first: true, empty: true,
        });
        renderPage();
        expect(await screen.findByText(/Kayıtlı deprem verisi bulunamadı/)).toBeInTheDocument();
    });

    it('Kandilli hatası admin\'e non-blocking uyarı gösterir, geri kalan sayfa çalışmaya devam eder', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
        (getEarthquakeDebugStatus as any).mockResolvedValue({ kandilliLastError: 'Bağlantı zaman aşımı' });
        renderPage();

        expect(await screen.findByText(/Kandilli verileri şu anda güncellenemiyor/)).toBeInTheDocument();
        // Sayfanın geri kalanı (tablo/başlık) hâlâ görünür — tam ekran hata değil.
        expect(screen.getByText('Depremler')).toBeInTheDocument();
        await waitFor(() => expect(getEarthquakes).toHaveBeenCalled());
    });
});

describe('EarthquakesPage — liste/harita senkronizasyonu', () => {
    it('liste ve harita aynı filtrelenmiş veri kümesini kullanır', async () => {
        const items = [eq({ id: 'a' }), eq({ id: 'b' }), eq({ id: 'c' })];
        (getEarthquakes as any).mockResolvedValue({
            content: items, totalElements: 3, totalPages: 1, size: 20, number: 0, last: true, first: true, empty: false,
        });
        renderPage();

        await waitFor(() => expect(getEarthquakes).toHaveBeenCalled());
        expect(await screen.findByTestId('earthquake-map-stub')).toHaveTextContent('3 nokta');
    });
});
