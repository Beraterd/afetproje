import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle, XCircle, AlertCircle, Users, MapPin, AlertTriangle } from 'lucide-react';

/**
 * Deprem e-postasındaki "Yakınlarıma Mesaj Gönder" linkinden yönlendirilen sonuç sayfası.
 * Backend 302 ile ?status=...&type=...&count=... parametreleriyle yönlendirir.
 * Oturum gerektirmez, mobile uyumludur.
 */
type ResultStatus = 'SUCCESS' | 'NO_CONTACTS' | 'EXPIRED' | 'INVALID' | 'ERROR';
type MessageType = 'DURUMUM_IYI' | 'TOPLANMA_ALANINA' | 'ENKAZ_ALTINDA' | null;

const STATUS_VALID = ['SUCCESS', 'NO_CONTACTS', 'EXPIRED', 'INVALID', 'ERROR'];

interface SuccessConfig {
    icon: React.ReactNode;
    title: string;
    subtitle: string;
    headerBg: string;
    iconBg: string;
    bodyBg: string;
    bodyBorder: string;
}

function getSuccessConfig(type: MessageType, count: number): SuccessConfig {
    switch (type) {
        case 'DURUMUM_IYI':
            return {
                icon: <CheckCircle className="h-12 w-12 text-green-600" />,
                title: 'Mesajınız Gönderildi 👍',
                subtitle: count > 0
                    ? `"Durumum İyi" bildirimi ${count} yakınınıza ulaştı.`
                    : '"Durumum İyi" bildiriminiz yakınlarınıza gönderildi.',
                headerBg: 'bg-green-700',
                iconBg: 'bg-green-100',
                bodyBg: 'bg-green-50',
                bodyBorder: 'border-green-200',
            };
        case 'TOPLANMA_ALANINA':
            return {
                icon: <MapPin className="h-12 w-12 text-blue-600" />,
                title: 'Mesajınız Gönderildi 📍',
                subtitle: count > 0
                    ? `"Toplanma Alanına Gidiyorum" bildirimi ${count} yakınınıza ulaştı.`
                    : '"Toplanma Alanına Gidiyorum" bildiriminiz yakınlarınıza gönderildi.',
                headerBg: 'bg-blue-700',
                iconBg: 'bg-blue-100',
                bodyBg: 'bg-blue-50',
                bodyBorder: 'border-blue-200',
            };
        case 'ENKAZ_ALTINDA':
            return {
                icon: <AlertTriangle className="h-12 w-12 text-red-600" />,
                title: 'Acil Durum Bildirimi Gönderildi 🚨',
                subtitle: count > 0
                    ? `"Enkaz Altındayım" acil bildirimi ${count} yakınınıza iletildi. Yardım geliyor!`
                    : '"Enkaz Altındayım" acil bildiriminiz yakınlarınıza gönderildi.',
                headerBg: 'bg-red-700',
                iconBg: 'bg-red-100',
                bodyBg: 'bg-red-50',
                bodyBorder: 'border-red-200',
            };
        default:
            return {
                icon: <CheckCircle className="h-12 w-12 text-green-600" />,
                title: 'Mesajınız Gönderildi',
                subtitle: count > 0
                    ? `Bildiriminiz ${count} yakınınıza ulaştı.`
                    : 'Bildiriminiz yakınlarınıza gönderildi.',
                headerBg: 'bg-green-700',
                iconBg: 'bg-green-100',
                bodyBg: 'bg-green-50',
                bodyBorder: 'border-green-200',
            };
    }
}

export const EmergencyMessageResultPage: React.FC = () => {
    const [params] = useSearchParams();
    const rawStatus = (params.get('status') || 'ERROR').toUpperCase();
    const status: ResultStatus = (STATUS_VALID.includes(rawStatus) ? rawStatus : 'ERROR') as ResultStatus;
    const type = (params.get('type')?.toUpperCase() || null) as MessageType;
    const count = Number(params.get('count') || '0');

    if (status === 'SUCCESS') {
        const cfg = getSuccessConfig(type, count);
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
                            {cfg.icon}
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-gray-900">{cfg.title}</h2>
                            <p className="text-gray-600 mt-2 text-sm leading-relaxed">{cfg.subtitle}</p>
                        </div>
                        <div className={`${cfg.bodyBg} border ${cfg.bodyBorder} rounded-lg px-4 py-3`}>
                            <p className="text-xs text-gray-600">
                                Yakınlarınız bilgilendirildi. Güvende kalın ve yetkililerin talimatlarını takip edin.
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

    const errorConfig: Record<Exclude<ResultStatus, 'SUCCESS'>, {
        icon: React.ReactNode; title: string; body: string; color: string;
    }> = {
        NO_CONTACTS: {
            icon: <Users className="h-10 w-10 text-amber-600" />,
            title: 'Kayıtlı Yakın Bulunamadı',
            body: 'Sisteme kayıtlı yakınınız bulunmamaktadır. Profil > Yakınlarım bölümünden yakın ekleyebilirsiniz.',
            color: 'text-amber-700',
        },
        EXPIRED: {
            icon: <AlertCircle className="h-10 w-10 text-orange-600" />,
            title: 'Bağlantı Süresi Doldu',
            body: 'Bu bağlantının geçerlilik süresi (24 saat) dolmuştur. Yeni bir deprem bildirimi e-postasındaki bağlantıyı kullanın.',
            color: 'text-orange-700',
        },
        INVALID: {
            icon: <XCircle className="h-10 w-10 text-red-600" />,
            title: 'Geçersiz Bağlantı',
            body: 'Bu bağlantı geçersiz veya daha önce kullanılmıştır. Her bağlantı yalnızca bir kez çalışır.',
            color: 'text-red-700',
        },
        ERROR: {
            icon: <XCircle className="h-10 w-10 text-red-600" />,
            title: 'Bir Hata Oluştu',
            body: 'İşlem sırasında beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.',
            color: 'text-red-700',
        },
    };

    const err = errorConfig[status];

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-xl max-w-md w-full overflow-hidden">
                <div className="bg-gray-800 px-6 py-4">
                    <p className="text-white font-semibold text-sm tracking-wide text-center">
                        AFET Koordinasyon Sistemi
                    </p>
                </div>
                <div className="p-8 text-center space-y-5">
                    <div className="bg-gray-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto">
                        {err.icon}
                    </div>
                    <div>
                        <h2 className={`text-xl font-bold ${err.color}`}>{err.title}</h2>
                        <p className="text-gray-600 mt-2 text-sm leading-relaxed">{err.body}</p>
                    </div>
                    <p className="text-xs text-gray-400">
                        Bu sayfa AFET Yönetim Sistemi tarafından güvenli şekilde oluşturulmuştur.
                    </p>
                </div>
            </div>
        </div>
    );
};
