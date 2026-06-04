import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle, XCircle, AlertCircle, Users } from 'lucide-react';

/**
 * §9 — Deprem e-postasındaki "Yakınlarıma Mesaj Gönder" linkinden gelen kullanıcının
 * yönlendirildiği güvenli sonuç sayfası. Backend işlemi tamamlayıp ?status=... ile yönlendirir;
 * bu sayfa yalnızca sonucu gösterir (ek API çağrısı yapmaz). Responsive.
 */
type ResultStatus = 'SUCCESS' | 'NO_CONTACTS' | 'EXPIRED' | 'INVALID' | 'ERROR';

const TITLES: Record<ResultStatus, string> = {
    SUCCESS: 'Mesajınız Gönderildi',
    NO_CONTACTS: 'Kayıtlı Yakın Bulunamadı',
    EXPIRED: 'Bağlantı Süresi Doldu',
    INVALID: 'Geçersiz Bağlantı',
    ERROR: 'Bir Hata Oluştu',
};

export const EmergencyMessageResultPage: React.FC = () => {
    const [params] = useSearchParams();
    const raw = (params.get('status') || 'ERROR').toUpperCase();
    const status: ResultStatus = (['SUCCESS', 'NO_CONTACTS', 'EXPIRED', 'INVALID', 'ERROR'].includes(raw)
        ? raw : 'ERROR') as ResultStatus;
    const count = Number(params.get('count') || '0');

    const renderIcon = () => {
        switch (status) {
            case 'SUCCESS':
                return <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center"><CheckCircle className="h-10 w-10 text-green-600" /></div>;
            case 'NO_CONTACTS':
                return <div className="bg-amber-100 w-16 h-16 rounded-full flex items-center justify-center"><Users className="h-10 w-10 text-amber-600" /></div>;
            case 'EXPIRED':
                return <div className="bg-orange-100 w-16 h-16 rounded-full flex items-center justify-center"><AlertCircle className="h-10 w-10 text-orange-600" /></div>;
            default:
                return <div className="bg-red-100 w-16 h-16 rounded-full flex items-center justify-center"><XCircle className="h-10 w-10 text-red-600" /></div>;
        }
    };

    const titleColor =
        status === 'SUCCESS' ? 'text-green-700'
        : status === 'NO_CONTACTS' ? 'text-amber-700'
        : status === 'EXPIRED' ? 'text-orange-700'
        : 'text-red-700';

    const renderBody = () => {
        switch (status) {
            case 'SUCCESS':
                return (
                    <p className="text-gray-600 mt-2">
                        Mesajınız seçili yakınlarınıza gönderildi.
                        {count > 0 && <> <strong>{count}</strong> kişiye ulaşıldı.</>}
                    </p>
                );
            case 'NO_CONTACTS':
                return (
                    <p className="text-gray-600 mt-2">
                        Kayıtlı yakınınız bulunamadı. <strong>Profil &gt; Yakınlarım</strong> bölümünden yakın ekleyebilirsiniz.
                    </p>
                );
            case 'EXPIRED':
                return <p className="text-gray-600 mt-2">Bu bağlantının süresi dolmuştur.</p>;
            case 'INVALID':
                return <p className="text-gray-600 mt-2">Bu bağlantı geçersiz veya daha önce kullanılmıştır.</p>;
            default:
                return <p className="text-gray-600 mt-2">İşlem sırasında bir hata oluştu. Lütfen daha sonra tekrar deneyin.</p>;
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full text-center space-y-6">
                <div className="flex justify-center">{renderIcon()}</div>
                <div>
                    <div className="bg-red-700 text-white text-sm font-semibold px-3 py-1 rounded-t-md -mx-8 -mt-6 mb-6">
                        AFET Yönetim Sistemi
                    </div>
                    <h2 className={`text-xl font-bold ${titleColor}`}>{TITLES[status]}</h2>
                    {renderBody()}
                </div>
                <p className="text-xs text-gray-400">
                    Bu bağlantı AFET Yönetim Sistemi tarafından güvenli şekilde oluşturulmuştur.
                </p>
            </div>
        </div>
    );
};
