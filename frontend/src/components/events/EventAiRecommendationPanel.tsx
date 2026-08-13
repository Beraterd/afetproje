import React, { useState } from 'react';
import { Sparkles, X, Mail, ChevronDown, ChevronUp } from 'lucide-react';
import { Button, LoadingSpinner } from '@/components/ui';
import type { RecommendedMemberResponse, TeamRecommendationResponse } from '@/types';
import { PROXIMITY_TR, AVAILABILITY_TR } from './eventDetailLabels';
import { AiBadge, AiDisclaimer } from '@/components/ai/AiDisclaimer';
import { useDemoMode } from '@/hooks/useDemoMode';

interface EventAiRecommendationPanelProps {
    recommendation: TeamRecommendationResponse | null;
    autoFetching: boolean;
    recommendPending: boolean;
    approvePending: boolean;
    rejectPending: boolean;
    selectedUserIds: Set<string>;
    requiredPeople: number;
    onToggleSelect: (userId: string) => void;
    onRequestRecommendation: () => void;
    onClearRecommendation: () => void;
    onApprove: () => void;
    onReject: () => void;
}

/** "AI Ekip Önerisi" — geçmiş görev/uygunluk analizine dayalı personel önerisi, seçim ve
 *  onay/red akışı (görev daveti gönderme). Mutasyonlar üst container'da (EventDetailPage) kalır,
 *  bu component yalnızca sunum + seçim state callback'lerini taşır. */
export const EventAiRecommendationPanel: React.FC<EventAiRecommendationPanelProps> = ({
    recommendation, autoFetching, recommendPending, approvePending, rejectPending,
    selectedUserIds, requiredPeople, onToggleSelect, onRequestRecommendation,
    onClearRecommendation, onApprove, onReject,
}) => {
    const [expandedReasonsUserId, setExpandedReasonsUserId] = useState<string | null>(null);
    const { isDemo, disabledReason } = useDemoMode();

    return (
        <div className="bg-white shadow sm:rounded-lg border border-blue-100 overflow-hidden">
            <div className="px-4 py-3 sm:px-6 flex items-center justify-between bg-blue-50 border-b border-blue-100">
                <div className="flex items-center gap-2">
                    <Sparkles className="h-5 w-5 text-blue-600" />
                    <h4 className="text-base font-semibold text-blue-900">AI Ekip Önerisi</h4>
                    <AiBadge />
                </div>
                {recommendation && (
                    <button onClick={onClearRecommendation} aria-label="Öneriyi kapat" className="text-gray-400 hover:text-gray-600">
                        <X className="h-4 w-4" />
                    </button>
                )}
            </div>

            <div className="px-4 py-4 sm:px-6">
                {!recommendation && !recommendPending && (
                    <div className="space-y-3">
                        <p className="text-sm text-gray-600">
                            Bu öneri, geçmiş tamamlanan görevler ve personel uygunluğu analiz edilerek otomatik üretilir.
                        </p>
                        <AiDisclaimer />
                        {autoFetching ? (
                            <div className="flex items-center gap-3 py-1">
                                <LoadingSpinner />
                                <span className="text-sm text-gray-600">
                                    Otomatik ekip önerisi hazırlanıyor...
                                </span>
                            </div>
                        ) : (
                            <p className="text-xs text-gray-500">
                                Otomatik öneri henüz hazır değil. Dilerseniz hemen yeni bir öneri oluşturabilirsiniz.
                            </p>
                        )}
                        <Button
                            variant="primary"
                            onClick={onRequestRecommendation}
                            leftIcon={<Sparkles className="h-4 w-4" />}
                            disabled={isDemo}
                            disabledReason={isDemo ? disabledReason : undefined}
                        >
                            AI ile Kişi Öner
                        </Button>
                    </div>
                )}

                {recommendPending && (
                    <div className="flex items-center gap-3 py-4">
                        <LoadingSpinner />
                        <span className="text-sm text-gray-600">
                            Geçmiş görevler ve personel uygunluğu analiz ediliyor...
                        </span>
                    </div>
                )}

                {recommendation && (
                    <div className="space-y-4">
                        <AiDisclaimer />
                        {recommendation.aiExplanation && (
                            <div className="bg-blue-50 border border-blue-200 rounded-md p-3 text-sm text-blue-800">
                                {recommendation.aiExplanation}
                            </div>
                        )}

                        {recommendation.recommendedPersonnel.length === 0 ? (
                            <div className="rounded-md border border-amber-200 bg-amber-50 px-4 py-3">
                                <p className="text-sm text-amber-800">
                                    Bu görev tipi için daha önce başarıyla tamamlanmış görevi olan
                                    uygun gönüllü bulunamadı. Aday havuzu ve geçmiş görev
                                    eşleşmeleri kontrol edilmelidir.
                                </p>
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200 text-sm">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th scope="col" className="px-3 py-2 w-8" />
                                            <th scope="col" className="px-3 py-2 text-left font-medium text-gray-500">Ad Soyad</th>
                                            <th scope="col" className="px-3 py-2 text-left font-medium text-gray-500">E-posta</th>
                                            <th scope="col" className="px-3 py-2 text-left font-medium text-gray-500">Skor</th>
                                            <th scope="col" className="px-3 py-2 text-left font-medium text-gray-500">Önceki Görev</th>
                                            <th scope="col" className="px-3 py-2 text-left font-medium text-gray-500">Yakınlık</th>
                                            <th scope="col" className="px-3 py-2 text-left font-medium text-gray-500">Durum</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100 bg-white">
                                        {recommendation.recommendedPersonnel.map((m: RecommendedMemberResponse) => {
                                            const reasonsOpen = expandedReasonsUserId === m.userId;
                                            return (
                                            // Güvenlik: VOLUNTEER olmayan kişiler backend'den gelmemeli,
                                            // ama gelirse gösterme
                                            <React.Fragment key={m.userId}>
                                            <tr
                                                className={selectedUserIds.has(m.userId) ? 'bg-blue-50' : ''}>
                                                <td className="px-3 py-2">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedUserIds.has(m.userId)}
                                                        onChange={() => onToggleSelect(m.userId)}
                                                        className="h-4 w-4 text-blue-600 rounded border-gray-300"
                                                    />
                                                </td>
                                                <td className="px-3 py-2 font-medium text-gray-900">
                                                    <div>{m.firstName} {m.lastName}</div>
                                                    {m.reasons.length > 0 && (
                                                        <button
                                                            type="button"
                                                            onClick={() => setExpandedReasonsUserId(reasonsOpen ? null : m.userId)}
                                                            aria-expanded={reasonsOpen}
                                                            className="mt-0.5 inline-flex items-center gap-0.5 text-xs font-normal text-blue-600 hover:text-blue-800"
                                                        >
                                                            Neden bu öneri?
                                                            {reasonsOpen ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                                                        </button>
                                                    )}
                                                </td>
                                                <td className="px-3 py-2 text-gray-600">{m.email}</td>
                                                <td className="px-3 py-2">
                                                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                                                        m.score >= 60 ? 'bg-green-100 text-green-800' :
                                                        m.score >= 30 ? 'bg-yellow-100 text-yellow-800' :
                                                        'bg-red-100 text-red-800'
                                                    }`}>
                                                        {m.score}
                                                    </span>
                                                </td>
                                                <td className="px-3 py-2 text-gray-500 max-w-xs truncate"
                                                    title={m.previousSimilarTask || '-'}>
                                                    {m.previousSimilarTask || '-'}
                                                </td>
                                                <td className="px-3 py-2 text-gray-500">
                                                    {PROXIMITY_TR[m.proximityLevel || 'NONE'] || '-'}
                                                </td>
                                                <td className="px-3 py-2">
                                                    <span className={`text-xs font-medium ${
                                                        m.availability === 'AVAILABLE'    ? 'text-green-600' :
                                                        m.availability === 'ACTIVE_TASK'  ? 'text-orange-600' :
                                                        'text-red-600'
                                                    }`}>
                                                        {AVAILABILITY_TR[m.availability || 'AVAILABLE'] || '-'}
                                                    </span>
                                                </td>
                                            </tr>
                                            {reasonsOpen && (
                                                <tr>
                                                    <td colSpan={7} className="px-3 pb-3 pt-0 bg-blue-50/40">
                                                        <ul className="space-y-0.5 pl-1">
                                                            {m.reasons.map((r, i) => (
                                                                <li key={i} className="text-xs text-gray-600">• {r}</li>
                                                            ))}
                                                        </ul>
                                                    </td>
                                                </tr>
                                            )}
                                            </React.Fragment>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        )}

                        <div className="flex items-center justify-between pt-2 border-t border-gray-100">
                            <span className="text-sm text-gray-600">
                                Seçilen: <strong>{selectedUserIds.size}</strong> / {requiredPeople}
                            </span>
                            <div className="flex gap-2">
                                <Button
                                    variant="danger"
                                    onClick={onReject}
                                    loading={rejectPending}
                                    disabled={isDemo}
                                    disabledReason={isDemo ? disabledReason : undefined}
                                >
                                    Öneriyi Reddet
                                </Button>
                                <Button
                                    variant="primary"
                                    disabled={selectedUserIds.size === 0 || isDemo}
                                    disabledReason={isDemo ? disabledReason : undefined}
                                    loading={approvePending}
                                    onClick={onApprove}
                                    leftIcon={<Mail className="h-4 w-4" />}
                                >
                                    Seçilen Kişilere Görev Maili Gönder
                                </Button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
