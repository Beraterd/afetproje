import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { acceptAssignmentToken } from '@/api/teamRecommendations.api';
import { CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react';

type ResultStatus = 'loading' | 'ACCEPTED' | 'ALREADY_ACCEPTED' | 'ALREADY_DECLINED' |
    'CANCELLED' | 'EVENT_INACTIVE' | 'TOKEN_EXPIRED' | 'error';

export const AssignmentAcceptPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const [status, setStatus] = useState<ResultStatus>('loading');
    const [message, setMessage] = useState('');
    const [eventTitle, setEventTitle] = useState('');

    useEffect(() => {
        if (!token) {
            setStatus('error');
            setMessage('Geçersiz davet bağlantısı. Token bulunamadı.');
            return;
        }
        acceptAssignmentToken(token)
            .then((data) => {
                const s = data.status as ResultStatus;
                setStatus(s);
                setMessage(data.message || '');
                if (data.eventTitle) setEventTitle(data.eventTitle);
            })
            .catch((err) => {
                setStatus('error');
                setMessage(err?.response?.data?.message || 'Bir hata oluştu. Lütfen tekrar deneyin.');
            });
    }, [token]);

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full text-center space-y-6">
                <div className="flex justify-center">
                    <div className="w-16 h-16 rounded-full flex items-center justify-center">
                        {status === 'loading' && (
                            <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                        )}
                        {status === 'ACCEPTED' && (
                            <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center">
                                <CheckCircle className="h-10 w-10 text-green-600" />
                            </div>
                        )}
                        {(status === 'ALREADY_ACCEPTED') && (
                            <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center">
                                <CheckCircle className="h-10 w-10 text-blue-600" />
                            </div>
                        )}
                        {(status === 'CANCELLED' || status === 'EVENT_INACTIVE' || status === 'ALREADY_DECLINED') && (
                            <div className="bg-orange-100 w-16 h-16 rounded-full flex items-center justify-center">
                                <AlertCircle className="h-10 w-10 text-orange-600" />
                            </div>
                        )}
                        {(status === 'TOKEN_EXPIRED' || status === 'error') && (
                            <div className="bg-red-100 w-16 h-16 rounded-full flex items-center justify-center">
                                <XCircle className="h-10 w-10 text-red-600" />
                            </div>
                        )}
                    </div>
                </div>

                <div>
                    <div className="bg-blue-700 text-white text-sm font-semibold px-3 py-1 rounded-t-md -mx-8 -mt-6 mb-6">
                        AFET Koordinasyon Sistemi
                    </div>

                    {status === 'loading' && (
                        <>
                            <h2 className="text-xl font-bold text-gray-900">İşleniyor...</h2>
                            <p className="text-gray-500 mt-2">Davet yanıtınız işleniyor, lütfen bekleyin.</p>
                        </>
                    )}

                    {status === 'ACCEPTED' && (
                        <>
                            <h2 className="text-xl font-bold text-green-700">Görevi Kabul Ettiniz</h2>
                            {eventTitle && (
                                <p className="text-gray-700 mt-2 font-medium">{eventTitle}</p>
                            )}
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}

                    {status === 'ALREADY_ACCEPTED' && (
                        <>
                            <h2 className="text-xl font-bold text-blue-700">Zaten Kabul Edildi</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}

                    {status === 'ALREADY_DECLINED' && (
                        <>
                            <h2 className="text-xl font-bold text-orange-700">Daha Önce Reddedildi</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}

                    {(status === 'CANCELLED' || status === 'EVENT_INACTIVE') && (
                        <>
                            <h2 className="text-xl font-bold text-orange-700">Görev Artık Aktif Değil</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}

                    {status === 'TOKEN_EXPIRED' && (
                        <>
                            <h2 className="text-xl font-bold text-red-700">Bağlantı Süresi Doldu</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                            <p className="text-gray-400 text-sm mt-2">Lütfen koordinatörünüzle iletişime geçin.</p>
                        </>
                    )}

                    {status === 'error' && (
                        <>
                            <h2 className="text-xl font-bold text-red-700">Bir Hata Oluştu</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}
                </div>

                <p className="text-xs text-gray-400">
                    Bu bağlantı AFET Koordinasyon Sistemi tarafından otomatik oluşturulmuştur.
                </p>
            </div>
        </div>
    );
};
