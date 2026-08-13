import React from 'react';
import { Brain, RefreshCw } from 'lucide-react';
import type { DamageAssessmentResponse } from '@/types';
import { aiConfidenceBadgeColor } from './damageDisplay';
import { AiBadge, AiDisclaimer } from '@/components/ai/AiDisclaimer';

interface DamageAiAnalysisProps {
    assessment: DamageAssessmentResponse;
    canRefresh: boolean;
    refreshing: boolean;
    onRefresh: () => void;
}

/** Hasar detayındaki "Yapay Zeka Ön Değerlendirmesi" bölümü — durum makinesi (PROCESSING/
 *  PENDING/COMPLETED/FAILED/yok) render'ını izole eder. API çağrısı yapmaz, yalnızca onRefresh
 *  callback'ini tetikler. */
export const DamageAiAnalysis: React.FC<DamageAiAnalysisProps> = ({
    assessment, canRefresh, refreshing, onRefresh,
}) => {
    return (
        <div className="border-t border-gray-100 pt-4">
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                    <Brain className="h-4 w-4 text-purple-600" />
                    <h3 className="text-sm font-medium text-gray-700">Yapay Zeka Ön Değerlendirmesi</h3>
                    <AiBadge />
                </div>
                {canRefresh && (
                    <button
                        onClick={onRefresh}
                        disabled={refreshing || assessment.aiAnalysisStatus === 'PROCESSING'}
                        className="flex items-center gap-1.5 text-xs text-purple-600 hover:text-purple-800 disabled:opacity-50 transition-colors"
                    >
                        <RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} />
                        {refreshing ? 'Ekleniyor...' : 'AI Yorumu Yenile'}
                    </button>
                )}
            </div>

            {assessment.aiAnalysisStatus === 'PROCESSING' && (
                <div className="bg-purple-50 rounded-lg px-4 py-3 text-sm text-purple-700 flex items-center gap-2">
                    <RefreshCw className="h-3.5 w-3.5 animate-spin flex-shrink-0" />
                    Analiz ediliyor...
                </div>
            )}

            {assessment.aiAnalysisStatus === 'PENDING' && (
                <div className="bg-yellow-50 rounded-lg px-4 py-3 text-sm text-yellow-700">
                    AI analizi kuyrukta, arka planda işlenecek.
                </div>
            )}

            {assessment.aiAnalysisStatus === 'COMPLETED' && assessment.aiComment && (
                <div className="space-y-3">
                    <div className="bg-purple-50 rounded-lg p-4 space-y-2">
                        <p className="text-sm text-gray-800">{assessment.aiComment}</p>
                        <div className="flex flex-wrap items-center gap-3 mt-2">
                            {assessment.aiConfidence && (
                                <div
                                    className="flex items-center gap-1.5"
                                    title="AI'ın hasar görüntüsünü ne kadar net ve ciddi değerlendirdiği — genel bir doğruluk güveni değildir"
                                >
                                    <span className="text-xs text-gray-500">Hasar Netliği:</span>
                                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${aiConfidenceBadgeColor(assessment.aiConfidence)}`}>
                                        {assessment.aiConfidenceLabel || assessment.aiConfidence}
                                    </span>
                                </div>
                            )}
                            {assessment.aiRiskScore != null && (
                                <div className="flex items-center gap-1.5">
                                    <span className="text-xs text-gray-500">Risk Skoru:</span>
                                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                                        assessment.aiRiskScore >= 70 ? 'bg-red-100 text-red-700'
                                        : assessment.aiRiskScore >= 40 ? 'bg-orange-100 text-orange-700'
                                        : 'bg-green-100 text-green-700'
                                    }`}>
                                        {assessment.aiRiskScore}/100
                                    </span>
                                </div>
                            )}
                            {assessment.aiPriority != null && (
                                <div className="flex items-center gap-1.5">
                                    <span className="text-xs text-gray-500">Öncelik:</span>
                                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
                                        {assessment.aiPriority}/5
                                    </span>
                                </div>
                            )}
                        </div>
                        {assessment.aiRecommendations && (
                            <div className="mt-2">
                                <p className="text-xs text-gray-500 mb-1">Önerilen Aksiyonlar:</p>
                                <ul className="space-y-0.5">
                                    {assessment.aiRecommendations.split(',').map((r, i) => (
                                        <li key={i} className="text-xs text-gray-700 flex items-start gap-1">
                                            <span className="text-purple-400 flex-shrink-0 mt-0.5">•</span>
                                            {r.trim()}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                        {assessment.aiAnalyzedAt && (
                            <p className="text-xs text-gray-400 mt-1">
                                {new Date(assessment.aiAnalyzedAt).toLocaleString('tr-TR')}
                                {assessment.aiModel && ` · ${assessment.aiModel}`}
                            </p>
                        )}
                    </div>
                    <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 space-y-1">
                        <div className="flex items-start gap-2">
                            <span className="text-amber-500 text-xs mt-0.5 flex-shrink-0">⚠</span>
                            <p className="text-xs text-amber-700">
                                Bu yorum yapay zekâ ön değerlendirmesidir. Resmi saha incelemesi yerine geçmez.
                            </p>
                        </div>
                        <AiDisclaimer className="text-amber-700 pl-4" />
                    </div>
                </div>
            )}

            {assessment.aiAnalysisStatus === 'FAILED' && (
                <div className="bg-red-50 rounded-lg px-4 py-3 text-sm text-red-600">
                    AI yorumu oluşturulamadı.
                    {canRefresh && ' "AI Yorumu Yenile" ile yeniden deneyebilirsiniz.'}
                </div>
            )}

            {(!assessment.aiAnalysisStatus || assessment.aiAnalysisStatus === 'NOT_STARTED') && (
                <p className="text-sm text-gray-400 italic">
                    AI analizi henüz yapılmamış; arka planda kuyruğa alındı.
                </p>
            )}
        </div>
    );
};
