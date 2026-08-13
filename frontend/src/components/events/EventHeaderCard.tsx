import React from 'react';
import { MapPin, Users, Calendar, AlertTriangle, Edit2, CheckCircle, UserCheck } from 'lucide-react';
import { format } from 'date-fns';
import { Button, Badge } from '@/components/ui';
import type { EventResponse, EventStatus } from '@/types';
import { TEAM_TR } from './eventDetailLabels';

function statusBadge(status: EventStatus) {
    switch (status) {
        case 'OPEN':
        case 'IN_PROGRESS': return <Badge variant="warning">Devam Ediyor</Badge>;
        case 'COMPLETED':   return <Badge variant="info">Tamamlandı</Badge>;
        case 'CLOSED':      return <Badge variant="neutral">İptal Edildi</Badge>;
        default:            return <Badge variant="neutral">{status}</Badge>;
    }
}

interface EventHeaderCardProps {
    event: EventResponse;
    canManage: boolean;
    isVolunteer: boolean;
    isActiveEvent: boolean;
    isJoined: boolean;
    acceptedCount: number;
    onEdit: () => void;
    onComplete: () => void;
    onClose: () => void;
    onJoin: () => void;
    onLeave: () => void;
    joinPending: boolean;
    leavePending: boolean;
}

/** Olay detayının üst kartı: başlık/durum, açıklama, yönetim/katılım aksiyonları ve konum/
 *  kadro/ekip/tarih bilgi listesi. Tüm mutasyonlar üst container'da (EventDetailPage) kalır. */
export const EventHeaderCard: React.FC<EventHeaderCardProps> = ({
    event, canManage, isVolunteer, isActiveEvent, isJoined, acceptedCount,
    onEdit, onComplete, onClose, onJoin, onLeave, joinPending, leavePending,
}) => {
    return (
        <div className="bg-white shadow sm:rounded-lg overflow-hidden border border-gray-200">
            <div className="px-4 py-5 sm:px-6 flex justify-between items-start gap-4">
                <div className="min-w-0">
                    <div className="flex items-center gap-3 flex-wrap">
                        <h3 className="text-2xl font-bold text-gray-900">{event.title}</h3>
                        {statusBadge(event.status)}
                    </div>
                    {event.description && (
                        <p className="mt-2 max-w-2xl text-sm text-gray-500 whitespace-pre-wrap">
                            {event.description}
                        </p>
                    )}
                </div>

                <div className="flex flex-wrap gap-2 justify-end shrink-0">
                    {canManage && isActiveEvent && (
                        <Button variant="secondary" onClick={onEdit} leftIcon={<Edit2 className="h-4 w-4" />}>
                            Düzenle
                        </Button>
                    )}
                    {canManage && isActiveEvent && (
                        <Button
                            variant="primary"
                            onClick={onComplete}
                            leftIcon={<CheckCircle className="h-4 w-4" />}
                        >
                            Tamamlandı
                        </Button>
                    )}
                    {canManage && isActiveEvent && (
                        <Button variant="danger" onClick={onClose}>
                            İptal Et
                        </Button>
                    )}

                    {isActiveEvent && isVolunteer && (
                        isJoined ? (
                            <button
                                disabled
                                className="inline-flex items-center gap-2 px-4 py-2 rounded-md
                                           bg-green-50 text-green-700 border border-green-200
                                           text-sm font-medium cursor-not-allowed"
                            >
                                <UserCheck className="h-4 w-4" />
                                Katıldınız
                            </button>
                        ) : (
                            <Button variant="primary" onClick={onJoin} loading={joinPending}>
                                Katıl
                            </Button>
                        )
                    )}

                    {isActiveEvent && isVolunteer && isJoined && event.assignedVolunteers > 0 && (
                        <Button variant="secondary" onClick={onLeave} loading={leavePending}>
                            Ayrıl
                        </Button>
                    )}
                </div>
            </div>

            <div className="border-t border-gray-200 px-4 py-5 sm:p-0">
                <dl className="sm:divide-y sm:divide-gray-200">
                    <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                        <dt className="text-sm font-medium text-gray-500 flex items-center">
                            <MapPin className="mr-2 h-4 w-4" /> Konum
                        </dt>
                        <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                            {event.neighborhood?.districtName} / {event.neighborhood?.name}
                        </dd>
                    </div>

                    <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                        <dt className="text-sm font-medium text-gray-500 flex items-center">
                            <Users className="mr-2 h-4 w-4" /> Kabul Eden / Gerekli Kişi
                        </dt>
                        <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0 flex items-center gap-2">
                            <span className={acceptedCount >= event.requiredPeople
                                ? 'text-green-600 font-semibold' : ''}>
                                {acceptedCount}
                            </span>
                            {' / '}{event.requiredPeople} kişi
                            {acceptedCount >= event.requiredPeople && (
                                <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full">
                                    Kadro Tamamlandı
                                </span>
                            )}
                        </dd>
                    </div>

                    <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                        <dt className="text-sm font-medium text-gray-500 flex items-center">
                            <AlertTriangle className="mr-2 h-4 w-4" /> Görevlendirilen Ekip
                        </dt>
                        <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                            {event.team
                            ? (() => {
                                const code = event.eventTeamCode || event.team.teamCode;
                                const label = TEAM_TR[event.team.name] || event.team.name;
                                return code ? `${code} — ${label}` : label;
                            })()
                            : '-'}
                        </dd>
                    </div>

                    {event.createdAt && (
                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500 flex items-center">
                                <Calendar className="mr-2 h-4 w-4" /> Oluşturulma Tarihi
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {format(new Date(event.createdAt), 'dd.MM.yyyy HH:mm')}
                            </dd>
                        </div>
                    )}

                    {event.closedAt && (
                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500 flex items-center">
                                <Calendar className="mr-2 h-4 w-4" />
                                {event.status === 'COMPLETED' ? 'Tamamlanma Tarihi' : 'Kapanma Tarihi'}
                            </dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {format(new Date(event.closedAt), 'dd.MM.yyyy HH:mm')}
                            </dd>
                        </div>
                    )}

                    {event.createdBy && (
                        <div className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-6">
                            <dt className="text-sm font-medium text-gray-500">Oluşturan</dt>
                            <dd className="mt-1 text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                {event.createdBy.firstName} {event.createdBy.lastName}
                            </dd>
                        </div>
                    )}
                </dl>
            </div>
        </div>
    );
};
