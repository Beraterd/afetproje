import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AiBadge, AiDisclaimer, AI_DISCLAIMER_TEXT } from '@/components/ai/AiDisclaimer';

describe('AiDisclaimer / AiBadge', () => {
    it('renders the standard AI disclaimer text', () => {
        render(<AiDisclaimer />);
        expect(screen.getByText(AI_DISCLAIMER_TEXT)).toBeInTheDocument();
    });

    it('renders an explicit "AI Önerisi" badge so AI content is clearly labelled', () => {
        render(<AiBadge />);
        expect(screen.getByText('AI Önerisi')).toBeInTheDocument();
    });
});
