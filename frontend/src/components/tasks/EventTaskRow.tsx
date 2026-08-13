import React from 'react';
import { Badge, Button } from '@/components/ui';
import type { UserEventResponse } from '@/types';
import { TEAM_TR, VOLUNTEER_STATUS_TR, VOLUNTEER_STATUS_VARIANT } from './taskLabels';

function eventStatusBadge(status: string) {
    switch (status) {
        case 'OPEN':
        case 'IN_PROGRESS': return <Badge variant="warning">Devam Ediyor</Badge>;
        case 'COMPLETED': return <Badge variant="info">Tamamlandı</Badge>;
        case 'CLOSED': return <Badge variant="neutral">İptal Edildi</Badge>;
        default: return <Badge variant="neutral">{status}</Badge>;
    }
}

export const EventTaskRow: React.FC<{
    ev: UserEventResponse;
    leavingEventId: string | null;
    onLeave: (id: string) => void;
    showLeaveButton?: boolean;
}> = ({ ev, leavingEventId, onLeave, showLeaveButton }) => {
    const canLeave = showLeaveButton
        && ev.volunteerStatus === 'ASSIGNED'
        && (ev.status === 'IN_PROGRESS' || ev.status === 'OPEN');

    return (
        <div className="px-4 py-4 sm:px-6 flex items-center justify-between gap-4">
            <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900">{ev.title}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                    {ev.neighborhood?.districtName} / {ev.neighborhood?.name}
                    {ev.team
                        ? ` — ${ev.team.teamCode ? `${ev.team.teamCode} ` : ''}${TEAM_TR[ev.team.name] || ev.team.name}`
                        : ''}
                </p>
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
                {eventStatusBadge(ev.status)}
                <Badge variant={VOLUNTEER_STATUS_VARIANT[ev.volunteerStatus] || 'neutral'}>
                    {VOLUNTEER_STATUS_TR[ev.volunteerStatus] || ev.volunteerStatus}
                </Badge>
                {canLeave && (
                    <Button
                        variant="danger"
                        size="sm"
                        loading={leavingEventId === ev.id}
                        disabled={leavingEventId !== null}
                        onClick={() => onLeave(ev.id)}
                    >
                        Görevi Bırak
                    </Button>
                )}
            </div>
        </div>
    );
};
