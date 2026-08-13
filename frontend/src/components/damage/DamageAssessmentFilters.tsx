import React from 'react';
import { Filter } from 'lucide-react';

interface DamageAssessmentFiltersProps {
    damageLevelFilter?: string;
    verificationStatusFilter?: string;
    onClear: () => void;
}

/** Bildirim/"Dikkat Gerektirenler" deep-link'lerinden gelen aktif filtreyi gösteren şerit. */
export const DamageAssessmentFilters: React.FC<DamageAssessmentFiltersProps> = ({
    damageLevelFilter, verificationStatusFilter, onClear,
}) => {
    if (!damageLevelFilter && !verificationStatusFilter) return null;

    return (
        <div className="flex items-center gap-2 text-sm bg-brand-50 border border-brand-200 text-brand-800 rounded-lg px-3 py-2">
            <Filter className="h-4 w-4 shrink-0" />
            <span>
                Filtre uygulandı
                {damageLevelFilter && <> · Hasar düzeyi: <strong>{damageLevelFilter}</strong></>}
                {verificationStatusFilter && <> · Durum: <strong>{verificationStatusFilter}</strong></>}
            </span>
            <button
                onClick={onClear}
                className="ml-auto text-xs font-medium text-brand-700 hover:text-brand-900 underline"
            >
                Filtreyi Temizle
            </button>
        </div>
    );
};
