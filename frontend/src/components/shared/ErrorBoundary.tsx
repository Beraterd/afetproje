import React from 'react';
import { AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui';

interface Props {
    children: React.ReactNode;
    /**
     * 'global': tüm ekranı kaplayan fatal hata sayfası.
     * 'section': sadece o widget'ı kaplayan izole kutu (Dashboard widget'ları, Timeline, AI paneli gibi blok içerikler için).
     * 'inline': nav bar gibi dar/yatay alanlarda kullanılan küçük ikon-buton (GlobalSearch, NotificationBell gibi kompakt chrome elemanları için — düzeni bozmaz).
     */
    level?: 'global' | 'section' | 'inline';
    /** section modunda kullanıcıya gösterilecek widget adı, örn. "Dikkat Gerektirenler". */
    sectionName?: string;
    onReset?: () => void;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

const CHUNK_RELOAD_FLAG = 'afet_chunk_reload_attempted';

function isChunkLoadError(error: Error): boolean {
    return (
        error.name === 'ChunkLoadError' ||
        /Loading chunk [\w-]+ failed/i.test(error.message) ||
        /Failed to fetch dynamically imported module/i.test(error.message) ||
        /error loading dynamically imported module/i.test(error.message)
    );
}

/**
 * Item 42 — hata izolasyonu. `level="section"` kritik widget sınırlarında (Dashboard
 * AttentionCenter, GlobalSearch, NotificationBell, AI panelleri, Timeline) kullanılır: alt
 * bileşen çökerse yalnızca o kutu "Tekrar Dene" gösterir, sayfanın geri kalanı etkilenmez.
 * `level="global"` (varsayılan) uygulama kökünde tek seferlik fatal hata ekranıdır.
 *
 * ChunkLoadError (deployment sonrası eski chunk hash'i) özel olarak ele alınır: sessionStorage
 * bayrağıyla EN FAZLA BİR KEZ otomatik `location.reload()` yapılır — sonsuz reload loop'u
 * oluşmaz, ikinci denemede normal hata ekranı gösterilir.
 */
export class ErrorBoundary extends React.Component<Props, State> {
    state: State = { hasError: false, error: null };

    static getDerivedStateFromError(error: Error): Partial<State> {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, info: React.ErrorInfo) {
        if (import.meta.env.DEV) {
            console.error('[ErrorBoundary]', error, info.componentStack);
        }

        if (isChunkLoadError(error)) {
            const alreadyAttempted = sessionStorage.getItem(CHUNK_RELOAD_FLAG) === '1';
            if (!alreadyAttempted) {
                sessionStorage.setItem(CHUNK_RELOAD_FLAG, '1');
                window.location.reload();
            }
        }
    }

    handleRetry = () => {
        this.setState({ hasError: false, error: null });
        this.props.onReset?.();
    };

    handleGoHome = () => {
        window.location.href = '/dashboard';
    };

    render() {
        if (!this.state.hasError) {
            return this.props.children;
        }

        if (this.props.level === 'inline') {
            return (
                <button
                    type="button"
                    onClick={this.handleRetry}
                    title={`${this.props.sectionName ?? 'Bileşen'} yüklenemedi. Tekrar denemek için tıklayın.`}
                    className="inline-flex items-center justify-center rounded-full p-2 text-red-500 hover:bg-red-50 transition-colors"
                >
                    <AlertTriangle className="h-5 w-5" />
                </button>
            );
        }

        if (this.props.level === 'section') {
            return (
                <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-4 text-sm text-red-800" role="alert">
                    <div className="flex items-center gap-2 font-medium">
                        <AlertTriangle className="h-4 w-4 flex-shrink-0" />
                        {this.props.sectionName
                            ? `${this.props.sectionName} yüklenirken bir hata oluştu.`
                            : 'Bu bölüm yüklenirken bir hata oluştu.'}
                    </div>
                    <Button variant="secondary" size="sm" className="mt-3" onClick={this.handleRetry}>
                        Tekrar Dene
                    </Button>
                </div>
            );
        }

        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
                <div className="max-w-md w-full text-center space-y-4">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-red-100">
                        <AlertTriangle className="h-7 w-7 text-red-600" />
                    </div>
                    <h1 className="text-xl font-semibold text-gray-900">Beklenmeyen bir hata oluştu.</h1>
                    <p className="text-sm text-gray-500">
                        Sayfa yüklenirken bir sorun oluştu. Sorun devam ederse teknik ekiple iletişime geçin.
                    </p>
                    {import.meta.env.DEV && this.state.error && (
                        <pre className="text-left text-xs bg-gray-100 text-gray-700 rounded p-3 overflow-auto max-h-40">
                            {this.state.error.stack || this.state.error.message}
                        </pre>
                    )}
                    <div className="flex gap-3 justify-center">
                        <Button variant="primary" onClick={this.handleRetry}>Tekrar Dene</Button>
                        <Button variant="secondary" onClick={this.handleGoHome}>Ana Sayfaya Dön</Button>
                    </div>
                </div>
            </div>
        );
    }
}
