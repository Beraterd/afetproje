import React, { useEffect, useRef } from 'react';
import { AlertCircle, Bot, Loader2, Sparkles, X } from 'lucide-react';
import type { OperationsAiResponse } from '@/api/operationsAi.api';
import { AiBadge, AiDisclaimer } from '@/components/ai/AiDisclaimer';

const QUICK_QUERIES: { label: string; prompt: string }[] = [
    {
        label: 'Son 24 saatin en kritik bölgeleri',
        prompt: 'Son 24 saat içinde en kritik bölgeler nelerdir? Hasar kayıtları, risk faktörleri ve kaynak taleplerini birlikte değerlendirerek öncelikli müdahale gerektiren bölgeleri listele.',
    },
    {
        label: 'En yoğun hasar alan ilçeler',
        prompt: 'En yoğun hasar alan ilçeler hangileri? İlçe bazlı hasar seviyesi dağılımını analiz et ve karşılaştır.',
    },
    {
        label: 'Riskli bölgeler',
        prompt: 'En riskli mahalle ve bölgeler hangileri? Çökme riski, gaz sızıntısı ve tahliye gerektiren durumları değerlendir.',
    },
    {
        label: 'Operasyon önerileri',
        prompt: 'Mevcut operasyonel duruma göre öncelikli aksiyonlar neler olmalı? Ekip ihtiyaçları ve koordinasyon önerilerini belirt.',
    },
    {
        label: 'Kaynak dağılım tavsiyeleri',
        prompt: 'Açık kaynak taleplerini ve mevcut kapasiteyi değerlendirerek kaynak dağılım tavsiyesi ver. Hangi bölgeye ne tür kaynak önceliklendirilmeli?',
    },
];

function renderAiAnswer(text: string): React.ReactNode {
    return text.split('\n').map((line, i) => {
        const trimmed = line.trim();
        if (trimmed.startsWith('## ')) {
            return (
                <p key={i} className="text-sm font-semibold text-gray-900 mt-4 mb-1 first:mt-0">
                    {trimmed.slice(3)}
                </p>
            );
        }
        if (trimmed.startsWith('- ') || trimmed.startsWith('• ')) {
            return (
                <p key={i} className="text-sm text-gray-700 pl-3 leading-relaxed">
                    {trimmed}
                </p>
            );
        }
        if (trimmed === '') {
            return <div key={i} className="h-1" />;
        }
        return <p key={i} className="text-sm text-gray-700 leading-relaxed">{line}</p>;
    });
}

interface AiDecisionSupportDrawerProps {
    open: boolean;
    onClose: () => void;
    prompt: string;
    onPromptChange: (value: string) => void;
    onSubmit: () => void;
    onQuickQuery: (prompt: string) => void;
    isPending: boolean;
    result: OperationsAiResponse | null;
    error: string | null;
    resultRef: React.RefObject<HTMLDivElement>;
}

/**
 * "AI Karar Destek" — masaüstünde sağdan açılan bir drawer, mobilde tam ekran bottom sheet.
 * Kapalıyken hiçbir yer kaplamaz; harita bu sayede varsayılan olarak tüm genişliği kullanır.
 * Tüm state/mutasyon MapPage'de tutulur, burada yalnızca sunum var.
 */
export const AiDecisionSupportDrawer: React.FC<AiDecisionSupportDrawerProps> = ({
    open, onClose, prompt, onPromptChange, onSubmit, onQuickQuery,
    isPending, result, error, resultRef,
}) => {
    const dialogRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!open) return;
        dialogRef.current?.focus();
        const handleKeydown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        document.addEventListener('keydown', handleKeydown);
        return () => document.removeEventListener('keydown', handleKeydown);
    }, [open, onClose]);

    if (!open) return null;

    return (
        <>
            <div
                className="fixed inset-0 bg-black/30 z-[1400] sm:bg-transparent sm:pointer-events-none"
                onClick={onClose}
                aria-hidden="true"
            />
            <div
                ref={dialogRef}
                tabIndex={-1}
                role="dialog"
                aria-modal="true"
                aria-label="AI Karar Destek"
                className="fixed inset-x-0 bottom-0 z-[1500] max-h-[85vh] rounded-t-2xl
                           sm:inset-x-auto sm:right-0 sm:top-0 sm:bottom-0 sm:max-h-none sm:h-full sm:w-[420px] sm:rounded-t-none sm:rounded-l-2xl
                           bg-white shadow-2xl border border-gray-200 overflow-y-auto focus:outline-none"
            >
                <div className="flex items-start gap-3 p-5 border-b border-gray-100 bg-gradient-to-r from-blue-50 to-indigo-50 sticky top-0 z-10">
                    <div className="p-2 bg-blue-600 rounded-lg flex-shrink-0">
                        <Bot className="h-5 w-5 text-white" />
                    </div>
                    <div className="min-w-0">
                        <div className="flex items-center gap-2">
                            <h2 className="text-base font-semibold text-gray-900">Operasyon Merkezi AI Asistanı</h2>
                            <AiBadge />
                        </div>
                        <p className="text-xs text-gray-500 mt-0.5">
                            Kritik bölgeler, hasar yoğunluğu, ekip ihtiyacı ve kaynak dağılımı hakkında öneriler üretir.
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        aria-label="AI Karar Destek panelini kapat"
                        className="ml-auto flex-shrink-0 text-gray-400 hover:text-gray-600"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <div className="p-5 space-y-4">
                    <div>
                        <p className="text-xs font-medium text-gray-500 mb-2">Hazır sorgular</p>
                        <div className="flex flex-wrap gap-2">
                            {QUICK_QUERIES.map((q) => (
                                <button
                                    key={q.label}
                                    onClick={() => onQuickQuery(q.prompt)}
                                    disabled={isPending}
                                    className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full border border-blue-200 bg-blue-50 text-blue-700 hover:bg-blue-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                >
                                    <Sparkles className="h-3 w-3" />
                                    {q.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div>
                        <label className="text-xs font-medium text-gray-500 block mb-1.5">Özel soru</label>
                        <textarea
                            value={prompt}
                            onChange={(e) => onPromptChange(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) onSubmit();
                            }}
                            placeholder="Operasyona dair sorunuzu yazın… (Ctrl+Enter ile gönder)"
                            rows={3}
                            className="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent placeholder:text-gray-400"
                        />
                    </div>

                    <button
                        onClick={onSubmit}
                        disabled={!prompt.trim() || isPending}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {isPending ? (
                            <>
                                <Loader2 className="h-4 w-4 animate-spin" />
                                Analiz ediliyor…
                            </>
                        ) : (
                            <>
                                <Bot className="h-4 w-4" />
                                AI Analiz Et
                            </>
                        )}
                    </button>

                    {isPending && (
                        <div className="animate-pulse space-y-2 pt-1">
                            <div className="h-3 bg-gray-200 rounded w-3/4" />
                            <div className="h-3 bg-gray-200 rounded w-full" />
                            <div className="h-3 bg-gray-200 rounded w-5/6" />
                            <div className="h-3 bg-gray-200 rounded w-2/3" />
                        </div>
                    )}

                    {error && !isPending && (
                        <div className="flex items-start gap-2 p-3 bg-red-50 border border-red-200 rounded-lg">
                            <AlertCircle className="h-4 w-4 text-red-500 flex-shrink-0 mt-0.5" />
                            <p className="text-sm text-red-700">{error}</p>
                        </div>
                    )}

                    {result && !isPending && (
                        <div ref={resultRef} className="border border-blue-100 rounded-lg overflow-hidden">
                            <div className="flex items-center justify-between px-4 py-2 bg-blue-50 border-b border-blue-100">
                                <div className="flex items-center gap-2">
                                    <Bot className="h-4 w-4 text-blue-600" />
                                    <span className="text-xs font-medium text-blue-700">AI Operasyon Analizi</span>
                                </div>
                                <span className="text-xs text-gray-400">
                                    {new Date(result.generatedAt).toLocaleTimeString('tr-TR')}
                                </span>
                            </div>
                            <div className="p-4 space-y-0.5">
                                {renderAiAnswer(result.answer)}
                            </div>
                            <div className="px-4 py-2 bg-gray-50 border-t border-gray-100">
                                <AiDisclaimer />
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
};
