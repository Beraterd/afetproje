import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Clock, Sparkles } from 'lucide-react';
import { getEventTimeline } from '@/api/events.api';
import { queryKeys } from '@/utils/queryKeys';

const AI_ACTION_TYPES = new Set(['TEAM_ASSIGNED']);

function formatDateTime(iso: string): string {
    return new Date(iso).toLocaleString('tr-TR', { dateStyle: 'medium', timeStyle: 'short' });
}

/**
 * Olay geçmişi — mevcut audit log kayıtlarından türetilir (backend: GET /events/{id}/timeline).
 * Yalnızca gerçekten kayıtlı aksiyonlar gösterilir, sahte/varsayımsal madde eklenmez.
 */
export const EventTimeline: React.FC<{ eventId: string }> = ({ eventId }) => {
    const { data: items = [], isLoading } = useQuery({
        queryKey: queryKeys.events.timeline(eventId),
        queryFn: () => getEventTimeline(eventId),
    });

    return (
        <div className="bg-white shadow sm:rounded-lg border border-gray-100">
            <div className="px-4 py-3 sm:px-6 flex items-center gap-2 border-b border-gray-100">
                <Clock className="h-5 w-5 text-gray-500" />
                <h4 className="text-base font-semibold text-gray-900">Zaman Çizelgesi</h4>
            </div>
            <div className="px-4 py-4 sm:px-6">
                {isLoading ? (
                    <p className="text-sm text-gray-500">Yükleniyor...</p>
                ) : items.length === 0 ? (
                    <p className="text-sm text-gray-400">Henüz kayıtlı bir işlem yok.</p>
                ) : (
                    <ol className="relative border-l border-gray-200 ml-2 space-y-5">
                        {items.map((item) => (
                            <li key={item.id} className="ml-4">
                                <span className="absolute -left-1.5 h-3 w-3 rounded-full bg-brand-500 border-2 border-white" />
                                <div className="flex items-center gap-1.5 flex-wrap">
                                    <p className="text-sm font-medium text-gray-900">{item.title}</p>
                                    {AI_ACTION_TYPES.has(item.type) && (
                                        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[10px] font-medium bg-blue-50 text-blue-700 border border-blue-200">
                                            <Sparkles className="h-2.5 w-2.5" aria-hidden="true" />
                                            AI
                                        </span>
                                    )}
                                </div>
                                {item.description && (
                                    <p className="text-sm text-gray-600 mt-0.5">{item.description}</p>
                                )}
                                <p className="text-xs text-gray-400 mt-0.5">
                                    {formatDateTime(item.createdAt)}
                                    {item.actorName ? ` — ${item.actorName}` : ''}
                                </p>
                            </li>
                        ))}
                    </ol>
                )}
            </div>
        </div>
    );
};
