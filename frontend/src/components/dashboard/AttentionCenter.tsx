import React from 'react';
import { Link } from 'react-router-dom';
import { AlertOctagon, AlertTriangle, Info, CheckCircle2, ChevronRight } from 'lucide-react';
import { useAttentionItems, AttentionSeverity } from '@/hooks/useAttentionItems';
import { LoadingSpinner } from '@/components/ui';

const SEVERITY_CONFIG: Record<AttentionSeverity, {
    icon: React.ComponentType<{ className?: string }>;
    badgeClass: string;
    iconClass: string;
    label: string;
}> = {
    CRITICAL: { icon: AlertOctagon,  badgeClass: 'bg-red-50 border-red-200',    iconClass: 'text-red-600',    label: 'Kritik' },
    HIGH:     { icon: AlertTriangle, badgeClass: 'bg-orange-50 border-orange-200', iconClass: 'text-orange-600', label: 'Yüksek' },
    MEDIUM:   { icon: AlertTriangle, badgeClass: 'bg-yellow-50 border-yellow-200', iconClass: 'text-yellow-700', label: 'Orta' },
    INFO:     { icon: Info,          badgeClass: 'bg-blue-50 border-blue-200',   iconClass: 'text-blue-600',   label: 'Bilgi' },
};

/**
 * Dashboard "Dikkat Gerektirenler" / aksiyon merkezi — "şu anda hangi konuya müdahale
 * etmeliyim?" sorusuna cevap verir. Sadece mevcut endpoint'lerden türetilen gerçek veriyi
 * gösterir (bkz. useAttentionItems). Yükleme/hata durumları dashboard'un geri kalanını etkilemez.
 */
export const AttentionCenter: React.FC = () => {
    const { items, isLoading, hasAnyError } = useAttentionItems();

    // Tüm kaynaklar başarısız olduysa sessizce gizlen — dashboard'un geri kalanı (KPI kartları)
    // bundan etkilenmemeli.
    if (hasAnyError && items.length === 0 && !isLoading) return null;

    return (
        <div className="glass-card px-4 py-5 sm:p-6">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-base font-semibold text-gray-900 flex items-center gap-2">
                    <AlertTriangle className="h-5 w-5 text-orange-600" aria-hidden="true" />
                    Dikkat Gerektirenler
                </h3>
                {isLoading && <LoadingSpinner size="sm" />}
            </div>

            {!isLoading && items.length === 0 ? (
                <div className="flex items-center gap-2 text-sm text-gray-500 py-2">
                    <CheckCircle2 className="h-5 w-5 text-green-500 shrink-0" aria-hidden="true" />
                    Şu anda dikkat gerektiren kritik bir durum bulunmuyor.
                </div>
            ) : (
                <ul className="space-y-2">
                    {items.map((item) => {
                        const config = SEVERITY_CONFIG[item.severity];
                        const Icon = config.icon;
                        return (
                            <li key={item.id}>
                                <Link
                                    to={item.relatedRoute}
                                    className={`flex items-center gap-3 rounded-xl border px-3 py-3 hover:shadow-sm transition-all ${config.badgeClass}`}
                                >
                                    <Icon className={`h-5 w-5 shrink-0 ${config.iconClass}`} aria-hidden="true" />
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium text-gray-900">{item.title}</p>
                                        <p className="text-xs text-gray-500 mt-0.5">{item.description}</p>
                                    </div>
                                    <span className={`shrink-0 text-[11px] font-semibold uppercase tracking-wide ${config.iconClass}`}>
                                        {config.label}
                                    </span>
                                    <ChevronRight className="h-4 w-4 text-gray-400 shrink-0" aria-hidden="true" />
                                </Link>
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
};
