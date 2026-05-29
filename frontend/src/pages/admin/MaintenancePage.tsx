import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { purgeOperationalData, PurgeResponse } from '@/api/users.api';
import { useToast } from '@/components/shared/ToastProvider';
import { Wrench, AlertTriangle, CheckCircle } from 'lucide-react';

const DEFAULT_PROTECTED_EMAILS = ['admin.gmail.com', 'admin@afetkoordinasyon.istanbul'];

export const MaintenancePage: React.FC = () => {
    const toast = useToast();

    const [purgeEvents, setPurgeEvents] = useState(false);
    const [purgeDamage, setPurgeDamage] = useState(false);
    const [purgeResources, setPurgeResources] = useState(false);
    const [purgeUsers, setPurgeUsers] = useState(false);
    const [protectedEmailsText, setProtectedEmailsText] = useState(DEFAULT_PROTECTED_EMAILS.join('\n'));
    const [confirmText, setConfirmText] = useState('');
    const [result, setResult] = useState<PurgeResponse | null>(null);

    const isConfirmed = confirmText === 'PURGE';
    const hasSelection = purgeEvents || purgeDamage || purgeResources || purgeUsers;

    const purgeMutation = useMutation({
        mutationFn: purgeOperationalData,
        onSuccess: (data) => {
            setResult(data);
            setConfirmText('');
            toast.success('Temizleme işlemi tamamlandı');
        },
        onError: (err: any) => {
            toast.error(err?.response?.data?.message || err.message || 'Temizleme işlemi başarısız');
        },
    });

    const handlePurge = () => {
        if (!isConfirmed || !hasSelection) return;
        const emails = protectedEmailsText
            .split('\n')
            .map(e => e.trim())
            .filter(e => e.length > 0);

        purgeMutation.mutate({
            purgeEvents,
            purgeDamageAssessments: purgeDamage,
            purgeResourceRequests: purgeResources,
            purgeUsers,
            protectedEmails: emails,
            confirmationText: confirmText,
        });
    };

    return (
        <div className="space-y-6 max-w-2xl">
            <div>
                <h1 className="text-2xl font-bold leading-7 text-gray-900 flex items-center">
                    <Wrench className="h-6 w-6 mr-2 text-orange-600" />
                    Sistem Bakımı
                </h1>
                <p className="mt-1 text-sm text-gray-500">
                    Operasyonel verileri toplu temizleme işlemleri. Bu işlemler geri alınamaz.
                </p>
            </div>

            {/* Uyarı banner */}
            <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-lg p-4">
                <AlertTriangle className="h-5 w-5 text-red-500 mt-0.5 shrink-0" />
                <p className="text-sm text-red-700">
                    <strong>Dikkat:</strong> Seçilen kategorilerdeki tüm kayıtlar kalıcı olarak silinir.
                    Protected e-posta listesindeki admin kullanıcılar asla silinmez.
                </p>
            </div>

            {/* Seçenekler */}
            <div className="bg-white rounded-xl shadow p-6 space-y-4">
                <h2 className="text-base font-semibold text-gray-900">Temizlenecek Kategoriler</h2>
                <div className="space-y-3">
                    {[
                        { label: 'Ekip durumu kayıtları (etkinlikler + gönüllüler)', value: purgeEvents, set: setPurgeEvents },
                        { label: 'Hasar tespiti kayıtları + fotoğraflar', value: purgeDamage, set: setPurgeDamage },
                        { label: 'Kaynak talepleri', value: purgeResources, set: setPurgeResources },
                        { label: 'Kullanıcılar (protected admin hariç)', value: purgeUsers, set: setPurgeUsers },
                    ].map(({ label, value, set }) => (
                        <label key={label} className="flex items-center gap-3 cursor-pointer">
                            <input
                                type="checkbox"
                                checked={value}
                                onChange={e => set(e.target.checked)}
                                className="h-4 w-4 rounded border-gray-300 text-red-600 focus:ring-red-500"
                            />
                            <span className="text-sm text-gray-700">{label}</span>
                        </label>
                    ))}
                </div>
            </div>

            {/* Protected e-postalar */}
            <div className="bg-white rounded-xl shadow p-6 space-y-2">
                <h2 className="text-base font-semibold text-gray-900">Korunan Admin E-postaları</h2>
                <p className="text-xs text-gray-500">Her satıra bir e-posta yazın. Bu kullanıcılar asla silinmez.</p>
                <textarea
                    value={protectedEmailsText}
                    onChange={e => setProtectedEmailsText(e.target.value)}
                    rows={3}
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-orange-500"
                />
            </div>

            {/* Onay */}
            <div className="bg-white rounded-xl shadow p-6 space-y-3">
                <h2 className="text-base font-semibold text-gray-900">Onay</h2>
                <p className="text-sm text-gray-600">
                    İşlemi başlatmak için aşağıya <code className="bg-gray-100 px-1 rounded font-bold">PURGE</code> yazın.
                </p>
                <input
                    type="text"
                    value={confirmText}
                    onChange={e => setConfirmText(e.target.value)}
                    placeholder="PURGE"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
                />
                <button
                    onClick={handlePurge}
                    disabled={!isConfirmed || !hasSelection || purgeMutation.isPending}
                    className="w-full py-2 px-4 bg-red-600 hover:bg-red-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-semibold rounded-lg text-sm transition-colors"
                >
                    {purgeMutation.isPending ? 'Temizleniyor...' : 'Sistemi Temizle'}
                </button>
            </div>

            {/* Sonuç */}
            {result && (
                <div className="bg-white rounded-xl shadow p-6 space-y-3">
                    <div className="flex items-center gap-2">
                        <CheckCircle className="h-5 w-5 text-green-500" />
                        <h2 className="text-base font-semibold text-gray-900">Temizleme Tamamlandı</h2>
                    </div>
                    <div className="grid grid-cols-2 gap-3 text-sm">
                        <StatItem label="Silinen gönüllü" value={result.deletedEventVolunteers} />
                        <StatItem label="Silinen etkinlik" value={result.deletedEvents} />
                        <StatItem label="Silinen hasar fotoğrafı" value={result.deletedDamagePhotos} />
                        <StatItem label="Silinen hasar tespiti" value={result.deletedDamageAssessments} />
                        <StatItem label="Silinen kaynak talebi" value={result.deletedResourceRequests} />
                        <StatItem label="Silinen kullanıcı" value={result.deletedUsers} />
                    </div>
                    {result.warnings && result.warnings.length > 0 && (
                        <div className="mt-3 space-y-1">
                            <p className="text-xs font-medium text-orange-700">Uyarılar:</p>
                            {result.warnings.map((w, i) => (
                                <p key={i} className="text-xs text-orange-600 bg-orange-50 rounded px-2 py-1">{w}</p>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

const StatItem: React.FC<{ label: string; value: number }> = ({ label, value }) => (
    <div className="bg-gray-50 rounded-lg px-3 py-2">
        <p className="text-xs text-gray-500">{label}</p>
        <p className="text-lg font-bold text-gray-900">{value}</p>
    </div>
);
