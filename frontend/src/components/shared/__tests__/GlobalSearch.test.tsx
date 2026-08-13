import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GlobalSearch } from '@/components/shared/GlobalSearch';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@/api/search.api', () => ({
    searchGlobal: vi.fn(),
}));

import { searchGlobal } from '@/api/search.api';

function renderSearch() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <GlobalSearch />
            </MemoryRouter>
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
});

describe('GlobalSearch', () => {
    it('does not call the API for a 1-character query', async () => {
        renderSearch();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'k');

        await new Promise((r) => setTimeout(r, 450));
        expect(searchGlobal).not.toHaveBeenCalled();
    });

    it('debounces and calls the API once a 2+ character query settles', async () => {
        (searchGlobal as any).mockResolvedValue({
            query: 'kadikoy',
            results: [{ type: 'EVENT', id: 'e1', title: 'Kadıköy bina çökmesi', subtitle: 'Caferağa Mahallesi' }],
        });
        renderSearch();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'kadikoy');

        await waitFor(() => expect(searchGlobal).toHaveBeenCalledWith('kadikoy'), { timeout: 1000 });
        expect(searchGlobal).toHaveBeenCalledTimes(1);
    });

    it('renders results grouped by category', async () => {
        (searchGlobal as any).mockResolvedValue({
            query: 'ka',
            results: [
                { type: 'EVENT', id: 'e1', title: 'Kadıköy bina çökmesi', subtitle: 'Caferağa Mahallesi' },
                { type: 'TEAM', id: 't1', title: 'AKUT-03', subtitle: 'Arama Kurtarma' },
            ],
        });
        renderSearch();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'ka');

        expect(await screen.findByText('Olaylar')).toBeInTheDocument();
        expect(screen.getByText('Ekipler')).toBeInTheDocument();
        expect(screen.getByText('Kadıköy bina çökmesi')).toBeInTheDocument();
        expect(screen.getByText('AKUT-03')).toBeInTheDocument();
    });

    it('navigates to the correct route when a result is clicked', async () => {
        (searchGlobal as any).mockResolvedValue({
            query: 'ka',
            results: [{ type: 'EVENT', id: 'e1', title: 'Kadıköy bina çökmesi' }],
        });
        renderSearch();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'ka');

        const result = await screen.findByText('Kadıköy bina çökmesi');
        await user.click(result);

        expect(mockNavigate).toHaveBeenCalledWith('/events/e1');
    });

    it('shows a no-results message', async () => {
        (searchGlobal as any).mockResolvedValue({ query: 'zzz', results: [] });
        renderSearch();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'zzz');

        expect(await screen.findByText('Sonuç bulunamadı.')).toBeInTheDocument();
    });

    it('shows an error message instead of crashing when the API fails', async () => {
        (searchGlobal as any).mockRejectedValue(new Error('network error'));
        renderSearch();
        const user = userEvent.setup();
        await user.type(screen.getByRole('combobox'), 'ka');

        expect(await screen.findByText('Arama şu anda kullanılamıyor.')).toBeInTheDocument();
    });
});
