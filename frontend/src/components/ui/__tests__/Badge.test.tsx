import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Badge } from '@/components/ui/Badge';

describe('Badge', () => {
    it('renders the high (orange) variant distinctly from warning (yellow/pending)', () => {
        render(<Badge variant="high">Yüksek</Badge>);
        const badge = screen.getByText('Yüksek');
        expect(badge.className).toContain('orange');
    });

    it('always renders visible text alongside color, not color-only', () => {
        render(<Badge variant="danger">Kritik</Badge>);
        expect(screen.getByText('Kritik')).toBeInTheDocument();
    });
});
