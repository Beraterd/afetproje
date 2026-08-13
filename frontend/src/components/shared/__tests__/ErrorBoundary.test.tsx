import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ErrorBoundary } from '@/components/shared/ErrorBoundary';

/** Yalnızca `shouldThrow.current` true iken render'da fırlatan test bileşeni — "Tekrar Dene"
 *  sonrası hatanın geçtiği bir senaryoyu simüle etmek için (gerçek uygulamada geçici bir hata
 *  durumu düzeldiğinde kullanıcı retry ile devam edebilir). */
function Bomb({ shouldThrow }: { shouldThrow: { current: boolean } }) {
    if (shouldThrow.current) {
        throw new Error('kaboom');
    }
    return <div>İçerik yüklendi</div>;
}

describe('ErrorBoundary', () => {
    beforeEach(() => {
        vi.spyOn(console, 'error').mockImplementation(() => {});
        sessionStorage.clear();
    });

    it('renders children normally when there is no error', () => {
        render(
            <ErrorBoundary>
                <div>Merhaba</div>
            </ErrorBoundary>,
        );
        expect(screen.getByText('Merhaba')).toBeInTheDocument();
    });

    it('shows the global fatal fallback with retry and go-home actions when a child throws', () => {
        const shouldThrow = { current: true };
        render(
            <ErrorBoundary level="global">
                <Bomb shouldThrow={shouldThrow} />
            </ErrorBoundary>,
        );

        expect(screen.getByText('Beklenmeyen bir hata oluştu.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Tekrar Dene' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Ana Sayfaya Dön' })).toBeInTheDocument();
    });

    it('retry re-renders children and recovers once the underlying error condition is gone', () => {
        const shouldThrow = { current: true };
        render(
            <ErrorBoundary level="global">
                <Bomb shouldThrow={shouldThrow} />
            </ErrorBoundary>,
        );
        expect(screen.getByText('Beklenmeyen bir hata oluştu.')).toBeInTheDocument();

        // Gerçek hata koşulu düzeldi (örn. geçici network sorunu), kullanıcı tekrar dener.
        shouldThrow.current = false;
        fireEvent.click(screen.getByRole('button', { name: 'Tekrar Dene' }));

        expect(screen.getByText('İçerik yüklendi')).toBeInTheDocument();
    });

    it('section-level boundary isolates the failure to its own box with a scoped message', () => {
        const shouldThrow = { current: true };
        render(
            <ErrorBoundary level="section" sectionName="Dikkat Gerektirenler">
                <Bomb shouldThrow={shouldThrow} />
            </ErrorBoundary>,
        );

        expect(screen.getByText('Dikkat Gerektirenler yüklenirken bir hata oluştu.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Tekrar Dene' })).toBeInTheDocument();
    });

    it('a crashing feature-level boundary does not take down a sibling widget outside it', () => {
        const shouldThrow = { current: true };
        render(
            <div>
                <ErrorBoundary level="section" sectionName="Çöken Widget">
                    <Bomb shouldThrow={shouldThrow} />
                </ErrorBoundary>
                <div>KPI kartı hâlâ görünür</div>
            </div>,
        );

        expect(screen.getByText('Çöken Widget yüklenirken bir hata oluştu.')).toBeInTheDocument();
        expect(screen.getByText('KPI kartı hâlâ görünür')).toBeInTheDocument();
    });

    it('inline boundary renders a compact retry icon-button that does not break nav layout', () => {
        const shouldThrow = { current: true };
        render(
            <ErrorBoundary level="inline" sectionName="Bildirimler">
                <Bomb shouldThrow={shouldThrow} />
            </ErrorBoundary>,
        );

        const retryButton = screen.getByRole('button');
        expect(retryButton).toHaveAttribute('title', expect.stringContaining('Bildirimler'));
    });
});
