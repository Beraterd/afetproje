import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ToastProvider } from '@/components/shared/ToastProvider';
import { useAuthStore } from '@/store/authStore';
import type { UserSummaryResponse } from '@/types';

vi.mock('@/api/resourceStocks.api', () => ({
    getResourceStocks: vi.fn(),
    getStockSummary: vi.fn(),
    createResourceStock: vi.fn(),
    updateStockQuantity: vi.fn(),
    getStockMovements: vi.fn(),
    getTransferSuggestions: vi.fn(),
    transferStock: vi.fn(),
}));
vi.mock('@/api/districts.api', () => ({ getDistricts: vi.fn() }));
vi.mock('@/api/neighborhoods.api', () => ({ getNeighborhoods: vi.fn() }));

import {
    getResourceStocks,
    getStockSummary,
    getTransferSuggestions,
    transferStock,
} from '@/api/resourceStocks.api';
import { getDistricts } from '@/api/districts.api';
import { StockSection } from '@/pages/resources/StockSection';

function makeUser(role: UserSummaryResponse['role']): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role, demo: false };
}

function baseStock(overrides: Record<string, any>) {
    return {
        id: overrides.id,
        name: overrides.name,
        category: 'WATER',
        categoryLabel: 'Su',
        quantity: overrides.quantity,
        unit: 'koli',
        criticalThreshold: overrides.criticalThreshold,
        status: overrides.status,
        statusLabel: overrides.statusLabel,
        districtId: 'd1',
        districtName: 'Kadıköy',
        active: true,
        updatedAt: '2026-01-01T00:00:00Z',
        ...overrides,
    };
}

beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: makeUser('ADMIN'), accessToken: 't' });
    (getDistricts as any).mockResolvedValue([]);
    (getStockSummary as any).mockResolvedValue({ totalItems: 2, criticalCount: 1, outOfStockCount: 0, averageDaysRemaining: null });
});

function renderSection() {
    return render(
        <ToastProvider>
            <StockSection />
        </ToastProvider>,
    );
}

describe('StockSection — critical prioritization', () => {
    it('renders critical/out-of-stock items before sufficient ones', async () => {
        (getResourceStocks as any).mockResolvedValue([
            baseStock({ id: 's1', name: 'Battaniye', quantity: 200, criticalThreshold: 50, status: 'SUFFICIENT', statusLabel: 'Yeterli' }),
            baseStock({ id: 's2', name: 'İçme Suyu', quantity: 63, criticalThreshold: 100, status: 'CRITICAL', statusLabel: 'Kritik' }),
        ]);
        renderSection();

        const names = (await screen.findAllByText(/Battaniye|İçme Suyu/)).map((el) => el.textContent);
        expect(names[0]).toBe('İçme Suyu');
        expect(names[1]).toBe('Battaniye');
    });

    it('shows the deficit amount for a critical item', async () => {
        (getResourceStocks as any).mockResolvedValue([
            baseStock({ id: 's2', name: 'İçme Suyu', quantity: 63, criticalThreshold: 100, status: 'CRITICAL', statusLabel: 'Kritik' }),
        ]);
        renderSection();

        expect(await screen.findByText('Eksik')).toBeInTheDocument();
        expect(screen.getByText('37 koli')).toBeInTheDocument();
    });

    it('does not show a deficit line for a sufficient item', async () => {
        (getResourceStocks as any).mockResolvedValue([
            baseStock({ id: 's1', name: 'Battaniye', quantity: 200, criticalThreshold: 50, status: 'SUFFICIENT', statusLabel: 'Yeterli' }),
        ]);
        renderSection();

        await screen.findByText('Battaniye');
        expect(screen.queryByText('Eksik')).not.toBeInTheDocument();
    });
});

describe('StockSection — transfer suggestion', () => {
    it('shows a deterministic transfer suggestion and applies it', async () => {
        (getResourceStocks as any).mockResolvedValue([
            baseStock({ id: 's2', name: 'İçme Suyu', quantity: 63, criticalThreshold: 100, status: 'CRITICAL', statusLabel: 'Kritik' }),
        ]);
        (getTransferSuggestions as any).mockResolvedValue([
            {
                sourceStockId: 's1', sourceLabel: 'İçme Suyu', sourceDistrictName: 'Üsküdar',
                availableQuantity: 350, suggestedQuantity: 37, unit: 'koli',
                reason: 'İçme Suyu 37 koli kritik eşik altında; Üsküdar deposunda güvenli fazlalık 350 koli.',
            },
        ]);
        (transferStock as any).mockResolvedValue({});
        renderSection();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /transfer önerisi/i }));
        expect(await screen.findByText(/Üsküdar.*37 koli transfer edilebilir/)).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: /transfer oluştur/i }));

        await waitFor(() => expect(transferStock).toHaveBeenCalledWith(
            expect.objectContaining({ sourceStockId: 's1', targetStockId: 's2', quantity: 37 }),
        ));
    });

    it('shows a no-suggestion message when none are available', async () => {
        (getResourceStocks as any).mockResolvedValue([
            baseStock({ id: 's2', name: 'İçme Suyu', quantity: 63, criticalThreshold: 100, status: 'CRITICAL', statusLabel: 'Kritik' }),
        ]);
        (getTransferSuggestions as any).mockResolvedValue([]);
        renderSection();
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /transfer önerisi/i }));
        expect(await screen.findByText('Bu kaynak için uygun bir transfer önerisi bulunamadı.')).toBeInTheDocument();
    });

    it('disables the transfer suggestion button in demo mode', async () => {
        useAuthStore.setState({ user: { ...makeUser('ADMIN'), demo: true }, accessToken: 't' });
        (getResourceStocks as any).mockResolvedValue([
            baseStock({ id: 's2', name: 'İçme Suyu', quantity: 63, criticalThreshold: 100, status: 'CRITICAL', statusLabel: 'Kritik' }),
        ]);
        renderSection();

        const btn = await screen.findByRole('button', { name: /transfer önerisi/i });
        expect(btn).toBeDisabled();
    });
});
