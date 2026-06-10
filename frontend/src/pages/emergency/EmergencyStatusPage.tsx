import React, { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { MapPin, CheckCircle, AlertTriangle, XCircle, Loader2, Navigation } from 'lucide-react';
import {
    validateEmergencyToken,
    sendEmergencyStatus,
    EmergencyTokenStatusResponse,
} from '@/api/emergency.api';
import { getQuickPosition, HighAccuracyResult, ACCURACY_WARNING_M, ACCURACY_REJECT_M } from '@/utils/geolocation';

type PageState =
    | 'loading'
    | 'confirm'
    | 'getting-location'
    | 'accuracy-confirm'   // 100-500m arası: kullanıcıya sor
    | 'location-choice'    // konum alınamadı, son bilinen konum varsa sun
    | 'sending'
    | 'success'
    | 'error';

type MessageType = 'DURUMUM_IYI' | 'TOPLANMA_ALANINA' | 'ENKAZ_ALTINDA';

const MSG_CONFIG: Record<MessageType, {
    label: string;
    color: string;
    headerBg: string;
    iconBg: string;
    Icon: React.ElementType;
}> = {
    DURUMUM_IYI: {
        label: 'Durumum İyi',
        color: 'text-green-700',
        headerBg: 'bg-green-700',
        iconBg: 'bg-green-100',
        Icon: CheckCircle,
    },
    TOPLANMA_ALANINA: {
        label: 'Toplanma Alanına Gidiyorum',
        color: 'text-blue-700',
        headerBg: 'bg-blue-700',
        iconBg: 'bg-blue-100',
        Icon: MapPin,
    },
    ENKAZ_ALTINDA: {
        label: 'Enkaz Altındayım',
        color: 'text-red-700',
        headerBg: 'bg-red-700',
        iconBg: 'bg-red-100',
        Icon: AlertTriangle,
    },
};

export const EmergencyStatusPage: React.FC = () => {
    const { token } = useParams<{ token: string }>();
    const [searchParams] = useSearchParams();
    const typeParam = (searchParams.get('type')?.toUpperCase() as MessageType) || 'DURUMUM_IYI';

    const [state, setState] = useState<PageState>('loading');
    const [tokenInfo, setTokenInfo] = useState<EmergencyTokenStatusResponse | null>(null);
    const [errorMsg, setErrorMsg] = useState('');
    const [sentCount, setSentCount] = useState(0);
    // Geçici olarak tutulan pozisyon (accuracy-confirm adımı için)
    const [pendingPos, setPendingPos] = useState<HighAccuracyResult | null>(null);

    const cfg = MSG_CONFIG[typeParam] ?? MSG_CONFIG.DURUMUM_IYI;

    useEffect(() => {
        if (!token) {
            setErrorMsg('Geçersiz bağlantı: token eksik.');
            setState('error');
            return;
        }
        validateEmergencyToken(token)
            .then((info) => {
                if (!info.valid) {
                    const MSGS: Record<string, string> = {
                        USED: 'Bu bağlantı daha önce kullanılmıştır. Her bağlantı yalnızca bir kez çalışır.',
                        EXPIRED: 'Bu bağlantının geçerlilik süresi (24 saat) dolmuştur.',
                        INVALID: 'Bu bağlantı geçersizdir.',
                    };
                    setErrorMsg(MSGS[info.errorCode ?? ''] ?? 'Geçersiz bağlantı.');
                    setState('error');
                } else {
                    setTokenInfo(info);
                    setState('confirm');
                }
            })
            .catch(() => {
                setErrorMsg('Token doğrulanırken bir hata oluştu. Lütfen tekrar deneyin.');
                setState('error');
            });
    }, [token]);

    const handleSend = () => {
        setState('getting-location');
        getQuickPosition()
            .then((pos) => {
                if (pos.accuracy > ACCURACY_REJECT_M) {
                    // 500m üstü → konum yok say
                    console.log(`[EmergencyPage] accuracy ±${Math.round(pos.accuracy)} m > ${ACCURACY_REJECT_M} m — konumsuz gönderiliyor`);
                    if (tokenInfo?.lastKnownLatitude && tokenInfo?.lastKnownLongitude) {
                        setState('location-choice');
                    } else {
                        doSend(null, null, null, null);
                    }
                } else if (pos.accuracy > ACCURACY_WARNING_M) {
                    // 100-500m → kullanıcıya sor
                    console.log(`[EmergencyPage] accuracy ±${Math.round(pos.accuracy)} m — onay bekleniyor`);
                    setPendingPos(pos);
                    setState('accuracy-confirm');
                } else {
                    // <= 100m → iyi konum, doğrudan gönder
                    console.log(`[EmergencyPage] accuracy ±${Math.round(pos.accuracy)} m — iyi kalite, gönderiliyor`);
                    doSend(pos.latitude, pos.longitude, pos.accuracy, 'CURRENT');
                }
            })
            .catch(() => {
                console.log('[EmergencyPage] Konum alınamadı — fallback kontrolü yapılıyor');
                if (tokenInfo?.lastKnownLatitude && tokenInfo?.lastKnownLongitude) {
                    setState('location-choice');
                } else {
                    doSend(null, null, null, null);
                }
            });
    };

    const doSend = (
        lat: number | null,
        lon: number | null,
        acc: number | null,
        source: string | null,
    ) => {
        setState('sending');
        sendEmergencyStatus({
            token: token!,
            type: typeParam,
            latitude: lat ?? undefined,
            longitude: lon ?? undefined,
            locationAccuracy: acc ?? undefined,
            locationSource: source ?? undefined,
            userAgent: navigator.userAgent,
        })
            .then((res) => {
                if (res.success) {
                    setSentCount(res.sentCount);
                    setState('success');
                } else {
                    const MSGS: Record<string, string> = {
                        EXPIRED: 'Bu bağlantının süresi dolmuştur.',
                        INVALID: 'Bu bağlantı geçersiz veya daha önce kullanılmıştır.',
                        ERROR: 'İşlem sırasında bir hata oluştu.',
                    };
                    setErrorMsg(MSGS[res.result] ?? 'İşlem başarısız.');
                    setState('error');
                }
            })
            .catch((err: unknown) => {
                const msg = err instanceof Error ? err.message : 'Mesaj gönderilemedi.';
                setErrorMsg(msg || 'Mesaj gönderilemedi. Lütfen tekrar deneyin.');
                setState('error');
            });
    };

    // ── Yükleniyor ─────────────────────────────────────────────────────────────

    if (state === 'loading') {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-8 text-center">
                    <Loader2 className="h-12 w-12 animate-spin text-blue-600 mx-auto mb-4" />
                    <p className="text-gray-600">Bağlantı doğrulanıyor…</p>
                </div>
            </div>
        );
    }

    // ── Hata ──────────────────────────────────────────────────────────────────

    if (state === 'error') {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                    <div className="bg-gray-800 px-6 py-4">
                        <p className="text-white font-semibold text-sm tracking-wide text-center">
                            AFET Koordinasyon Sistemi
                        </p>
                    </div>
                    <div className="p-8 text-center space-y-5">
                        <div className="bg-red-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto">
                            <XCircle className="h-10 w-10 text-red-600" />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-red-700">Bağlantı Kullanılamıyor</h2>
                            <p className="text-gray-600 mt-2 text-sm leading-relaxed">{errorMsg}</p>
                        </div>
                        <p className="text-xs text-gray-400">
                            Bu sayfa AFET Yönetim Sistemi tarafından güvenli şekilde oluşturulmuştur.
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    // ── Başarılı ──────────────────────────────────────────────────────────────

    if (state === 'success') {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                    <div className={`${cfg.headerBg} px-6 py-4`}>
                        <p className="text-white font-semibold text-sm tracking-wide text-center">
                            AFET Koordinasyon Sistemi
                        </p>
                    </div>
                    <div className="p-8 text-center space-y-5">
                        <div className={`${cfg.iconBg} w-20 h-20 rounded-full flex items-center justify-center mx-auto`}>
                            <cfg.Icon className={`h-12 w-12 ${cfg.color}`} />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-gray-900">Mesajınız Gönderildi</h2>
                            <p className="text-gray-600 mt-2 text-sm leading-relaxed">
                                &quot;{cfg.label}&quot; bildirimi{' '}
                                {sentCount > 0 ? `${sentCount} yakınınıza iletildi.` : 'yakınlarınıza gönderildi.'}
                            </p>
                        </div>
                        <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-3">
                            <p className="text-xs text-gray-600">
                                Güvende kalın ve yetkililerin talimatlarını takip edin.
                            </p>
                        </div>
                        <p className="text-xs text-gray-400">
                            Bu bağlantı tek kullanımlıktır ve tekrar çalışmayacaktır.
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    // ── accuracy-confirm: 100-500m arası, kullanıcıya sor ─────────────────────

    if (state === 'accuracy-confirm') {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                    <div className={`${cfg.headerBg} px-6 py-4`}>
                        <p className="text-white font-semibold text-sm tracking-wide text-center">
                            AFET Koordinasyon Sistemi
                        </p>
                    </div>
                    <div className="p-6 space-y-4">
                        <div className="flex items-start gap-3 bg-amber-50 border border-amber-200 rounded-lg p-3">
                            <AlertTriangle className="h-5 w-5 text-amber-600 mt-0.5 flex-shrink-0" />
                            <div>
                                <p className="text-sm font-semibold text-amber-700">Düşük Konum Hassasiyeti</p>
                                <p className="text-xs text-amber-600 mt-1">
                                    Alınan konum hassasiyeti ±{Math.round(pendingPos?.accuracy ?? 0)} m ({pendingPos?.source}).
                                    Bu değer düşük kaliteli olabilir ve gerçek konumunuzdan sapabilir.
                                </p>
                            </div>
                        </div>
                        <p className="text-sm text-gray-700 text-center">
                            Konum hassasiyeti düşük. Yine de bu konumla göndermek istiyor musunuz?
                        </p>
                        <button
                            onClick={() =>
                                doSend(
                                    pendingPos!.latitude,
                                    pendingPos!.longitude,
                                    pendingPos!.accuracy,
                                    'CURRENT',
                                )
                            }
                            className="w-full py-3 px-4 bg-amber-500 text-white rounded-xl font-semibold hover:bg-amber-600 transition-colors"
                        >
                            Yine de Gönder (±{Math.round(pendingPos?.accuracy ?? 0)} m)
                        </button>
                        <button
                            onClick={() => doSend(null, null, null, null)}
                            className="w-full py-3 px-4 bg-gray-100 text-gray-700 rounded-xl font-semibold hover:bg-gray-200 transition-colors"
                        >
                            Konumsuz Gönder
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ── location-choice: konum alınamadı, son bilinen konum öner ──────────────

    if (state === 'location-choice') {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                    <div className={`${cfg.headerBg} px-6 py-4`}>
                        <p className="text-white font-semibold text-sm tracking-wide text-center">
                            AFET Koordinasyon Sistemi
                        </p>
                    </div>
                    <div className="p-6 space-y-4">
                        <h2 className="text-lg font-bold text-gray-900 text-center">Güncel Konum Alınamadı</h2>
                        <p className="text-sm text-gray-600 text-center">
                            Tarayıcı konumunuza erişemedi. Ne yapmak istersiniz?
                        </p>
                        <button
                            onClick={() =>
                                doSend(
                                    tokenInfo!.lastKnownLatitude!,
                                    tokenInfo!.lastKnownLongitude!,
                                    tokenInfo!.lastKnownLocationAccuracy ?? null,
                                    'LAST_KNOWN',
                                )
                            }
                            className="w-full py-3 px-4 bg-blue-600 text-white rounded-xl font-semibold hover:bg-blue-700 transition-colors"
                        >
                            Son Bilinen Konumla Gönder
                        </button>
                        <button
                            onClick={() => doSend(null, null, null, null)}
                            className="w-full py-3 px-4 bg-gray-100 text-gray-700 rounded-xl font-semibold hover:bg-gray-200 transition-colors"
                        >
                            Konumsuz Gönder
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ── Onay ekranı (confirm / getting-location / sending) ────────────────────

    const isBusy = state === 'getting-location' || state === 'sending';

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                <div className={`${cfg.headerBg} px-6 py-4`}>
                    <p className="text-white font-semibold text-sm tracking-wide text-center">
                        AFET Koordinasyon Sistemi
                    </p>
                </div>

                <div className="p-6 space-y-5">
                    <div className="text-center">
                        <div
                            className={`w-16 h-16 rounded-full ${cfg.iconBg} flex items-center justify-center mx-auto mb-3`}
                        >
                            <cfg.Icon className={`h-10 w-10 ${cfg.color}`} />
                        </div>
                        <h2 className="text-lg font-bold text-gray-900">Afet Durum Bildirimi</h2>
                        {tokenInfo?.senderDisplayName && (
                            <p className="text-sm text-gray-500 mt-1">
                                Gönderen: {tokenInfo.senderDisplayName}
                            </p>
                        )}
                    </div>

                    <div className="rounded-lg p-4 border bg-gray-50">
                        <p className={`font-semibold ${cfg.color} text-sm text-center`}>Seçilen Durum</p>
                        <p className={`font-bold ${cfg.color} text-base text-center mt-1`}>{cfg.label}</p>
                    </div>

                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                        <div className="flex items-start gap-2">
                            <Navigation className="h-4 w-4 text-blue-600 mt-0.5 flex-shrink-0" />
                            <p className="text-xs text-blue-700">
                                Gönder butonuna basıldığında güncel konumunuz alınmaya çalışılacak ve
                                yakınlarınıza iletilecektir. Konum paylaşımı zorunlu değildir.
                            </p>
                        </div>
                    </div>

                    <button
                        onClick={handleSend}
                        disabled={isBusy}
                        className={`w-full py-3 px-6 rounded-xl font-bold text-white text-base transition-all
                            ${isBusy ? 'bg-gray-400 cursor-not-allowed' : `${cfg.headerBg} hover:opacity-90`}`}
                    >
                        {state === 'getting-location' ? (
                            <span className="flex items-center justify-center gap-2">
                                <Loader2 className="h-5 w-5 animate-spin" /> Konum alınıyor…
                            </span>
                        ) : state === 'sending' ? (
                            <span className="flex items-center justify-center gap-2">
                                <Loader2 className="h-5 w-5 animate-spin" /> Gönderiliyor…
                            </span>
                        ) : (
                            'Gönder'
                        )}
                    </button>

                    <p className="text-xs text-gray-400 text-center">
                        Bu bağlantı 24 saat geçerlidir ve yalnızca bir kez kullanılabilir.
                    </p>
                </div>
            </div>
        </div>
    );
};
