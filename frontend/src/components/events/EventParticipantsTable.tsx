import React from 'react';
import { Users } from 'lucide-react';
import { format } from 'date-fns';
import type { EventParticipantResponse } from '@/types';
import { PARTICIPANT_STATUS_TR } from './eventDetailLabels';

interface EventParticipantsTableProps {
    participants: EventParticipantResponse[];
}

/** "Göreve Katılanlar" tablosu — doğrudan katılım ve AI daveti kaynaklı katılımcıları birlikte
 *  gösterir. Salt sunum; veri üst container'dan gelir. */
export const EventParticipantsTable: React.FC<EventParticipantsTableProps> = ({ participants }) => {
    if (participants.length === 0) return null;

    const activeParticipants = participants.filter(
        (p) => p.status === 'JOINED' || p.status === 'ACCEPTED',
    );

    return (
        <div className="bg-white shadow sm:rounded-lg border border-gray-200 overflow-hidden">
            <div className="px-4 py-3 sm:px-6 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
                <Users className="h-4 w-4 text-gray-600" />
                <h4 className="text-sm font-semibold text-gray-800">
                    Göreve Katılanlar
                    <span className="ml-2 text-xs text-gray-500 font-normal">
                        ({activeParticipants.length} aktif / {participants.length} toplam)
                    </span>
                </h4>
            </div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-4 py-2 text-left font-medium text-gray-500">Ad Soyad</th>
                            <th className="px-4 py-2 text-left font-medium text-gray-500">E-posta</th>
                            <th className="px-4 py-2 text-left font-medium text-gray-500">Kaynak</th>
                            <th className="px-4 py-2 text-left font-medium text-gray-500">Durum</th>
                            <th className="px-4 py-2 text-left font-medium text-gray-500">Tarih</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100 bg-white">
                        {participants.map((p) => {
                            const st = PARTICIPANT_STATUS_TR[p.status] ?? { label: p.status, color: 'text-gray-600 bg-gray-50' };
                            return (
                                <tr key={`${p.userId}-${p.source}`}>
                                    <td className="px-4 py-2 font-medium text-gray-900">
                                        {p.firstName} {p.lastName}
                                    </td>
                                    <td className="px-4 py-2 text-gray-600">{p.email}</td>
                                    <td className="px-4 py-2">
                                        <span className={`text-xs px-2 py-0.5 rounded-full ${
                                            p.source === 'DIRECT_JOIN'
                                                ? 'bg-purple-50 text-purple-700'
                                                : 'bg-blue-50 text-blue-700'
                                        }`}>
                                            {p.source === 'DIRECT_JOIN' ? 'Doğrudan' : 'AI Davet'}
                                        </span>
                                    </td>
                                    <td className="px-4 py-2">
                                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${st.color}`}>
                                            {st.label}
                                        </span>
                                    </td>
                                    <td className="px-4 py-2 text-gray-500 text-xs">
                                        {p.joinedAt
                                            ? format(new Date(p.joinedAt), 'dd.MM.yyyy HH:mm')
                                            : '-'}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};
