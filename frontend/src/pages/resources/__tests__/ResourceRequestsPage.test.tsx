import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { ToastProvider } from '@/components/shared/ToastProvider';
import type { UserSummaryResponse } from '@/types';

vi.mock('@/api/resourceRequests.api', () => ({
    getResourceRequests: vi.fn(),
    createResourceRequest: vi.fn(),
    updateResourceRequestStatus: vi.fn(),
}));
vi.mock('@/api/resourceStocks.api', () => ({
    lookupStock: vi.fn(),
}));
vi.mock('@/api/districts.api', () => ({ getDistricts: vi.fn() }));
vi.mock('@/api/neighborhoods.api', () => ({ getNeighborhoods: vi.fn() }));
vi.mock('../StockSection', () => ({ StockSection: () => <div data-testid="stock-section-stub" /> }));

import { getResourceRequests } from '@/api/resourceRequests.api';
import { getDistricts } from '@/api/districts.api';
import { ResourceRequestsPage } from '@/pages/resources/ResourceRequestsPage';

function makeUser(role: UserSummaryResponse['role'], demo = false): UserSummaryResponse {
    return { id: 'u1', firstName: 'Ada', lastName: 'Lovelace', email: 'a@b.com', role, demo };
}

function renderPage(initialEntries: string[] = ['/resource-requests']) {
    return render(
        <MemoryRouter initialEntries={initialEntries}>
            <ToastProvider>
                <ResourceRequestsPage />
            </ToastProvider>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    (getResourceRequests as any).mockResolvedValue({ content: [], totalPages: 0 });
    (getDistricts as any).mockResolvedValue([]);
});

describe('ResourceRequestsPage — demo mode', () => {
    it('lets a demo user view the (read-only) request list', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN', true), accessToken: 't' });
        renderPage();

        await waitFor(() => expect(getResourceRequests).toHaveBeenCalled());
        expect(screen.getByText('Kaynak Talepleri ve Stok')).toBeInTheDocument();
    });

    it('disables the create button for a demo user, with a reason exposed via title', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN', true), accessToken: 't' });
        renderPage();

        const createButton = await screen.findByRole('button', { name: /yeni talep aç/i });
        expect(createButton).toBeDisabled();
        expect(createButton).toHaveAttribute('title', 'Demo modunda düzenleme yapılamaz.');
    });

    it('leaves the create button enabled for a non-demo user with the same role', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN', false), accessToken: 't' });
        renderPage();

        const createButton = await screen.findByRole('button', { name: /yeni talep aç/i });
        expect(createButton).not.toBeDisabled();
    });
});

describe('ResourceRequestsPage — attention-center deep link', () => {
    it('scrolls the stock section into view when ?section=stock is present', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN', false), accessToken: 't' });
        const scrollSpy = vi.fn();
        Element.prototype.scrollIntoView = scrollSpy;

        renderPage(['/resource-requests?section=stock']);

        await waitFor(() => expect(getResourceRequests).toHaveBeenCalled());
        await waitFor(() => expect(scrollSpy).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' }));
    });

    it('does not scroll when no section param is present', async () => {
        useAuthStore.setState({ user: makeUser('ADMIN', false), accessToken: 't' });
        const scrollSpy = vi.fn();
        Element.prototype.scrollIntoView = scrollSpy;

        renderPage();

        await waitFor(() => expect(getResourceRequests).toHaveBeenCalled());
        expect(scrollSpy).not.toHaveBeenCalled();
    });
});
