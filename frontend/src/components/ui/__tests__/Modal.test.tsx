import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Modal } from '@/components/ui/Modal';

describe('Modal', () => {
    it('renders as a semantic dialog with aria-modal and a labelled title', () => {
        render(
            <Modal isOpen onClose={() => {}} title="Test Başlığı">
                <p>İçerik</p>
            </Modal>,
        );
        const dialog = screen.getByRole('dialog', { name: 'Test Başlığı' });
        expect(dialog).toHaveAttribute('aria-modal', 'true');
    });

    it('closes when Escape is pressed', () => {
        const onClose = vi.fn();
        render(
            <Modal isOpen onClose={onClose} title="Test Başlığı">
                <p>İçerik</p>
            </Modal>,
        );
        fireEvent.keyDown(document, { key: 'Escape' });
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('returns focus to the triggering element when closed', () => {
        const trigger = document.createElement('button');
        trigger.textContent = 'Aç';
        document.body.appendChild(trigger);
        trigger.focus();
        expect(document.activeElement).toBe(trigger);

        const { rerender } = render(
            <Modal isOpen onClose={() => {}} title="Test Başlığı">
                <p>İçerik</p>
            </Modal>,
        );
        expect(document.activeElement).not.toBe(trigger);

        rerender(
            <Modal isOpen={false} onClose={() => {}} title="Test Başlığı">
                <p>İçerik</p>
            </Modal>,
        );
        expect(document.activeElement).toBe(trigger);
        document.body.removeChild(trigger);
    });

    it('close button has an accessible name', () => {
        render(
            <Modal isOpen onClose={() => {}} title="Test Başlığı">
                <p>İçerik</p>
            </Modal>,
        );
        expect(screen.getByRole('button', { name: 'Kapat' })).toBeInTheDocument();
    });
});
