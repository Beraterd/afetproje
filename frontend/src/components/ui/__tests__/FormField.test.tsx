import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FormField } from '@/components/ui/FormField';

describe('FormField', () => {
    it('associates the label with the input via htmlFor/id', () => {
        render(
            <FormField label="Ad">
                <input />
            </FormField>,
        );
        const input = screen.getByLabelText('Ad');
        expect(input).toBeInTheDocument();
    });

    it('preserves an externally supplied id instead of overwriting it', () => {
        render(
            <FormField label="Ad">
                <input id="custom-id" />
            </FormField>,
        );
        const input = screen.getByLabelText('Ad');
        expect(input).toHaveAttribute('id', 'custom-id');
    });

    it('links a validation error to the input via aria-invalid/aria-describedby', () => {
        render(
            <FormField label="E-posta" error="Geçerli bir e-posta girin">
                <input />
            </FormField>,
        );
        const input = screen.getByLabelText('E-posta');
        expect(input).toHaveAttribute('aria-invalid', 'true');
        const describedBy = input.getAttribute('aria-describedby');
        expect(describedBy).toBeTruthy();
        const errorEl = document.getElementById(describedBy!);
        expect(errorEl).toHaveTextContent('Geçerli bir e-posta girin');
    });

    it('links help text to the input when there is no error', () => {
        render(
            <FormField label="Telefon" hint="5xx xxx xx xx formatında girin">
                <input />
            </FormField>,
        );
        const input = screen.getByLabelText('Telefon');
        expect(input).toHaveAttribute('aria-invalid', 'false');
        const describedBy = input.getAttribute('aria-describedby');
        expect(describedBy).toBeTruthy();
        const hintEl = document.getElementById(describedBy!);
        expect(hintEl).toHaveTextContent('5xx xxx xx xx formatında girin');
    });
});
