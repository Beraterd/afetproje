import React from 'react';
import { Building2, MapPin, Navigation, Eye, BadgeCheck, UserPlus } from 'lucide-react';
import type { DamageAssessmentResponse } from '@/types';
import { googleMapsUrl, damageLevelColor, verificationStatusColor } from './damageDisplay';

const AI_STATUS_BADGE: Record<string, { label: string; className: string; pulse?: boolean }> = {
    COMPLETED: { label: 'Hazır', className: 'bg-purple-100 text-purple-700' },
    PROCESSING: { label: 'Analiz Ediliyor', className: 'bg-blue-100 text-blue-700', pulse: true },
    PENDING: { label: 'Kuyrukta', className: 'bg-yellow-100 text-yellow-700' },
    FAILED: { label: 'Hata', className: 'bg-red-100 text-red-600' },
};

interface DamageAssessmentListProps {
    assessments: DamageAssessmentResponse[];
    loading: boolean;
    page: number;
    totalPages: number;
    canVerify: boolean;
    onPageChange: (page: number) => void;
    onViewDetail: (assessment: DamageAssessmentResponse) => void;
    onVerify: (assessment: DamageAssessmentResponse) => void;
    onAssign: (assessment: DamageAssessmentResponse) => void;
}

/** Hasar tespiti listesi — tablo + sayfalama. Tüm veri/aksiyonlar üst container'dan (page) gelir,
 *  bu component herhangi bir API çağrısı yapmaz. */
export const DamageAssessmentList: React.FC<DamageAssessmentListProps> = ({
    assessments, loading, page, totalPages, canVerify,
    onPageChange, onViewDetail, onVerify, onAssign,
}) => {
    if (loading) {
        return <p className="text-center text-gray-500 py-10">Yükleniyor...</p>;
    }

    if (assessments.length === 0) {
        return (
            <div className="text-center py-16">
                <Building2 className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500">Kayıt bulunamadı</p>
            </div>
        );
    }

    return (
        <>
            <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                            <th className="text-left px-4 py-3 font-medium text-gray-600">Adres</th>
                            <th className="text-left px-4 py-3 font-medium text-gray-600">Mahalle / İlçe</th>
                            <th className="text-left px-4 py-3 font-medium text-gray-600">Hasar Düzeyi</th>
                            <th className="text-left px-4 py-3 font-medium text-gray-600">Durum</th>
                            <th className="text-left px-4 py-3 font-medium text-gray-600">AI Durumu</th>
                            <th className="text-left px-4 py-3 font-medium text-gray-600">Raporlayan</th>
                            <th className="px-4 py-3"></th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {assessments.map((a) => {
                            const aiBadge = a.aiAnalysisStatus ? AI_STATUS_BADGE[a.aiAnalysisStatus] : undefined;
                            return (
                                <tr key={a.id} className="hover:bg-gray-50">
                                    <td className="px-4 py-3">
                                        <div className="flex items-center gap-1.5">
                                            {a.latitude && a.longitude ? (
                                                <a
                                                    href={googleMapsUrl(a.latitude, a.longitude)}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    title="Google Maps'te yol tarifi al"
                                                    className="flex-shrink-0 text-blue-500 hover:text-blue-700 transition-colors"
                                                    onClick={(e) => e.stopPropagation()}
                                                >
                                                    <Navigation className="h-3.5 w-3.5" />
                                                </a>
                                            ) : (
                                                <MapPin className="h-3.5 w-3.5 flex-shrink-0 text-gray-300" />
                                            )}
                                            <span className="text-gray-800 line-clamp-1 max-w-[200px]">{a.address}</span>
                                        </div>
                                    </td>
                                    <td className="px-4 py-3 text-gray-600 text-xs">
                                        {a.neighborhoodName}, {a.districtName}
                                    </td>
                                    <td className="px-4 py-3">
                                        <span className={`px-2 py-0.5 rounded-full text-xs ${damageLevelColor(a.damageLevel)}`}>
                                            {a.damageLevelLabel}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">
                                        <span className={`px-2 py-0.5 rounded-full text-xs ${verificationStatusColor(a.verificationStatus)}`}>
                                            {a.verificationStatusLabel}
                                        </span>
                                    </td>
                                    <td className="px-4 py-3">
                                        {aiBadge ? (
                                            <span className={`px-2 py-0.5 rounded-full text-xs ${aiBadge.className} ${aiBadge.pulse ? 'animate-pulse' : ''}`}>
                                                {aiBadge.label}
                                            </span>
                                        ) : (
                                            <span className="px-2 py-0.5 rounded-full text-xs bg-gray-100 text-gray-400">—</span>
                                        )}
                                    </td>
                                    <td className="px-4 py-3 text-gray-500 text-xs">{a.reportedBy || '-'}</td>
                                    <td className="px-4 py-3 flex items-center gap-1">
                                        <button
                                            onClick={() => onViewDetail(a)}
                                            className="p-1.5 text-gray-400 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                                            title="Detayları Gör"
                                        >
                                            <Eye className="h-4 w-4" />
                                        </button>
                                        {canVerify && (
                                            <button
                                                onClick={() => onVerify(a)}
                                                className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                title="Durumu Güncelle"
                                            >
                                                <BadgeCheck className="h-4 w-4" />
                                            </button>
                                        )}
                                        {canVerify && (
                                            <button
                                                onClick={() => onAssign(a)}
                                                className="p-1.5 text-gray-400 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                                                title="Görevli Yönlendir"
                                            >
                                                <UserPlus className="h-4 w-4" />
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
            {totalPages > 1 && (
                <div className="flex justify-center gap-2">
                    <button disabled={page === 0} onClick={() => onPageChange(page - 1)} className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40">Önceki</button>
                    <span className="px-3 py-1.5 text-sm text-gray-500">{page + 1} / {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)} className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40">Sonraki</button>
                </div>
            )}
        </>
    );
};
