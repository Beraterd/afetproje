import React from 'react';
import { Sparkles } from 'lucide-react';
import { cn } from '@/utils/cn';

export const AI_DISCLAIMER_TEXT =
    'AI tarafından üretilen öneridir. Nihai operasyon kararı yetkili koordinatöre aittir.';

/** AI tarafından üretilen içerikleri açıkça işaretlemek için küçük etiket. */
export const AiBadge: React.FC<{ className?: string; label?: string }> = ({
    className,
    label = 'AI Önerisi',
}) => (
    <span
        className={cn(
            'inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-blue-50 text-blue-700 border border-blue-200',
            className,
        )}
    >
        <Sparkles className="h-3 w-3" aria-hidden="true" />
        {label}
    </span>
);

/**
 * AI karar destek yüzeylerinde (Operasyon Merkezi AI Asistanı, ekip önerisi, hasar AI analizi)
 * tekrarlanan sabit uyarı metni için tek ortak component — her kartta ayrı ayrı paragraf
 * yazılmasın diye.
 */
export const AiDisclaimer: React.FC<{ className?: string }> = ({ className }) => (
    <p className={cn('text-xs text-gray-400', className)} role="note">
        {AI_DISCLAIMER_TEXT}
    </p>
);
