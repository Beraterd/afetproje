import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/api/events.api', () => ({
    getEventTimeline: vi.fn(),
}));

import { getEventTimeline } from '@/api/events.api';
import { EventTimeline } from '@/components/events/EventTimeline';

function renderTimeline(eventId = 'evt-1') {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return render(
        <QueryClientProvider client={queryClient}>
            <EventTimeline eventId={eventId} />
        </QueryClientProvider>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
});

describe('EventTimeline', () => {
    it('shows an empty-state message when there is no recorded history', async () => {
        (getEventTimeline as any).mockResolvedValue([]);
        renderTimeline();
        expect(await screen.findByText('Henüz kayıtlı bir işlem yok.')).toBeInTheDocument();
    });

    it('renders real timeline items with actor and timestamp', async () => {
        (getEventTimeline as any).mockResolvedValue([
            {
                id: 't1', type: 'EVENT_CREATED', title: 'Olay oluşturuldu',
                description: 'Olay kaydı oluşturuldu', actorName: 'Ahmet Yılmaz',
                createdAt: '2026-01-01T08:00:00Z',
            },
        ]);
        renderTimeline();

        expect(await screen.findByText('Olay oluşturuldu')).toBeInTheDocument();
        expect(screen.getByText('Olay kaydı oluşturuldu')).toBeInTheDocument();
        expect(screen.getByText(/Ahmet Yılmaz/)).toBeInTheDocument();
    });

    it('marks an AI-sourced action (TEAM_ASSIGNED) with an AI badge, and shows the approving human actor', async () => {
        (getEventTimeline as any).mockResolvedValue([
            {
                id: 't2', type: 'TEAM_ASSIGNED', title: 'Ekip görevlendirildi',
                description: 'AI ekip önerisi onaylandı ve seçilen kişilere görev daveti gönderildi',
                actorName: 'Ayşe Koordinatör', createdAt: '2026-01-01T09:00:00Z',
            },
        ]);
        renderTimeline();

        expect(await screen.findByText('AI')).toBeInTheDocument();
        expect(screen.getByText(/AI ekip önerisi onaylandı/)).toBeInTheDocument();
        expect(screen.getByText(/Ayşe Koordinatör/)).toBeInTheDocument();
    });

    it('renders items in the order the API returns them (newest first, per backend contract)', async () => {
        (getEventTimeline as any).mockResolvedValue([
            { id: 't2', type: 'EVENT_JOINED', title: 'Gönüllü katıldı', actorName: null, createdAt: '2026-01-01T10:00:00Z' },
            { id: 't1', type: 'EVENT_CREATED', title: 'Olay oluşturuldu', actorName: null, createdAt: '2026-01-01T08:00:00Z' },
        ]);
        renderTimeline();

        const titles = (await screen.findAllByText(/Gönüllü katıldı|Olay oluşturuldu/)).map((el) => el.textContent);
        expect(titles).toEqual(['Gönüllü katıldı', 'Olay oluşturuldu']);
    });
});
