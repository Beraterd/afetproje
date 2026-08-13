import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from '@/components/ui/Button';

describe('Button — disabledReason', () => {
    it('surfaces the disabled reason via title and an accessible description when disabled', () => {
        render(
            <Button disabled disabledReason="Demo modunda düzenleme yapılamaz.">
                Kaydet
            </Button>,
        );
        const button = screen.getByRole('button', { name: 'Kaydet' });
        expect(button).toBeDisabled();
        expect(button).toHaveAttribute('title', 'Demo modunda düzenleme yapılamaz.');
        const describedBy = button.getAttribute('aria-describedby');
        expect(describedBy).toBeTruthy();
        expect(document.getElementById(describedBy!)).toHaveTextContent('Demo modunda düzenleme yapılamaz.');
    });

    it('does not add a reason description when enabled', () => {
        render(
            <Button disabledReason="Demo modunda düzenleme yapılamaz.">
                Kaydet
            </Button>,
        );
        const button = screen.getByRole('button', { name: 'Kaydet' });
        expect(button).not.toBeDisabled();
        expect(button).not.toHaveAttribute('aria-describedby');
    });
});
