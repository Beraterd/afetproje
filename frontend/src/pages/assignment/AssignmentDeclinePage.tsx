import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { declineAssignmentToken } from '@/api/teamRecommendations.api';
import { CheckCircle, XCircle, AlertCircle } from 'lucide-react';

type ResultStatus = 'loading' | 'DECLINED' | 'ALREADY_ACCEPTED' | 'ALREADY_DECLINED' |
    'CANCELLED' | 'TOKEN_EXPIRED' | 'error';

export const AssignmentDeclinePage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const [status, setStatus] = useState<ResultStatus>('loading');
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (!token) {
            setStatus('error');
            setMessage('Geçersiz davet bağlantısı. Token bulunamadı.');
            return;
        }
        declineAssignmentToken(token)
            .then((data) => {
                setStatus(data.status as ResultStatus);
                setMessage(data.message || '');
            })
            .catch((err) => {
                setStatus('error');
                setMessage(err?.response?.data?.message || 'Bir hata oluştu.');
            });
    }, [token]);

    return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-lg p-8 max-w-md w-full text-center space-y-6">
                <div className="flex justify-center">
                    {status === 'loading' && (
                        <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                    )}
                    {status === 'DECLINED' && (
                        <div className="bg-gray-100 w-16 h-16 rounded-full flex items-center justify-center">
                            <CheckCircle className="h-10 w-10 text-gray-600" />
                        </div>
                    )}
                    {(status === 'CANCELLED' || status === 'ALREADY_DECLINED') && (
                        <div className="bg-orange-100 w-16 h-16 rounded-full flex items-center justify-center">
                            <AlertCircle className="h-10 w-10 text-orange-600" />
                        </div>
                    )}
                    {(status === 'TOKEN_EXPIRED' || status === 'error' || status === 'ALREADY_ACCEPTED') && (
                        <div className="bg-red-100 w-16 h-16 rounded-full flex items-center justify-center">
                            <XCircle className="h-10 w-10 text-red-600" />
                        </div>
                    )}
                </div>

                <div>
                    {status === 'loading' && <h2 className="text-xl font-bold text-gray-900">İşleniyor...</h2>}
                    {status === 'DECLINED' && (
                        <>
                            <h2 className="text-xl font-bold text-gray-700">Davet Reddedildi</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}
                    {status !== 'loading' && status !== 'DECLINED' && (
                        <>
                            <h2 className="text-xl font-bold text-orange-700">İşlem Tamamlanamadı</h2>
                            <p className="text-gray-500 mt-2">{message}</p>
                        </>
                    )}
                </div>

                <p className="text-xs text-gray-400">
                    AFET Koordinasyon Sistemi
                </p>
            </div>
        </div>
    );
};
