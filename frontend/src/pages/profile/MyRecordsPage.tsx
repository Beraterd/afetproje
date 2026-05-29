import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ClipboardList, Building2, MapPin, Navigation, Eye, X, BadgeCheck } from 'lucide-react';
import { getMyDamageAssessments } from '@/api/damageAssessments.api';
import { DamageAssessmentResponse, DAMAGE_LEVELS, VERIFICATION_STATUSES } from '@/types';
import { LoadingSpinner } from '@/components/ui';

function damageLevelColor(level: string) {
    switch (level) {
        case 'LIGHT': return 'bg-yellow-100 text-yellow-800';
        case 'MODERATE': return 'bg-orange-100 text-orange-800';
        case 'HEAVY': return 'bg-red-100 text-red-800';
        case 'COLLAPSED': return 'bg-red-200 text-red-900 font-bold';
        default: return 'bg-gray-100 text-gray-600';
    }
}

function verificationStatusColor(status: string) {
    switch (status) {
        case 'INCELEME_GEREKIYOR': return 'bg-yellow-100 text-yellow-800';
        case 'SAHADA_DOGRULANDI': return 'bg-blue-100 text-blue-800';
        case 'KOORDINATOR_ONAYLADI': return 'bg-green-100 text-green-800';
        default: return 'bg-gray-100 text-gray-600';
    }
}

function googleMapsUrl(lat: number, lng: number) {
    return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

export const MyRecordsPage: React.FC = () => {
    const [page, setPage] = useState(0);
    const [showDetail, setShowDetail] = useState<DamageAssessmentResponse | null>(null);

    const { data, isLoading, isError } = useQuery({
        queryKey: ['damage-assessments', 'my', page],
        queryFn: () => getMyDamageAssessments({ page, size: 15 }),
    });

    const assessments = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;

    return (
        <div className="max-w-4xl mx-auto space-y-5">
            <div className="flex items-center gap-3">
                <ClipboardList className="h-7 w-7 text-indigo-600" />
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Kayıtlarım</h1>
                    <p className="text-sm text-gray-500 mt-0.5">Tarafınızdan oluşturulan hasar tespit kayıtları</p>
                </div>
            </div>

            {isLoading ? (
                <div className="flex justify-center py-16">
                    <LoadingSpinner size="lg" label="Kayıtlar yükleniyor..." />
                </div>
            ) : isError ? (
                <div className="text-center py-16">
                    <p className="text-red-500">Kayıtlar yüklenirken bir hata oluştu.</p>
                </div>
            ) : assessments.length === 0 ? (
                <div className="text-center py-16">
                    <Building2 className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                    <p className="text-gray-500 font-medium">Henüz hiç hasar tespiti kaydı oluşturmadınız.</p>
                    <p className="text-sm text-gray-400 mt-1">
                        Hasar Tespiti sayfasından yeni kayıt oluşturabilirsiniz.
                    </p>
                </div>
            ) : (
                <>
                    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
                        <table className="w-full text-sm">
                            <thead className="bg-gray-50 border-b border-gray-200">
                                <tr>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Adres</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Mahalle / İlçe</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Hasar</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Durum</th>
                                    <th className="text-left px-4 py-3 font-medium text-gray-600">Tarih</th>
                                    <th className="px-4 py-3"></th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {assessments.map(a => (
                                    <tr key={a.id} className="hover:bg-gray-50">
                                        <td className="px-4 py-3">
                                            <div className="flex items-center gap-1.5">
                                                {a.latitude && a.longitude ? (
                                                    <a
                                                        href={googleMapsUrl(a.latitude, a.longitude)}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        title="Google Maps'te göster"
                                                        className="flex-shrink-0 text-blue-500 hover:text-blue-700"
                                                        onClick={e => e.stopPropagation()}
                                                    >
                                                        <Navigation className="h-3.5 w-3.5" />
                                                    </a>
                                                ) : (
                                                    <MapPin className="h-3.5 w-3.5 text-gray-300 flex-shrink-0" />
                                                )}
                                                <span className="text-gray-800 line-clamp-1 max-w-[180px]">{a.address}</span>
                                            </div>
                                        </td>
                                        <td className="px-4 py-3 text-gray-500 text-xs">
                                            {a.neighborhoodName}, {a.districtName}
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-0.5 rounded-full text-xs ${damageLevelColor(a.damageLevel)}`}>
                                                {a.damageLevelLabel}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3">
                                            <span className={`px-2 py-0.5 rounded-full text-xs ${verificationStatusColor(a.verificationStatus)}`}>
                                                {a.verificationStatusLabel}
                                            </span>
                                        </td>
                                        <td className="px-4 py-3 text-gray-400 text-xs whitespace-nowrap">
                                            {new Date(a.createdAt).toLocaleDateString('tr-TR')}
                                        </td>
                                        <td className="px-4 py-3">
                                            <button
                                                onClick={() => setShowDetail(a)}
                                                className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                                                title="Detayları Gör"
                                            >
                                                <Eye className="h-4 w-4" />
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {totalPages > 1 && (
                        <div className="flex justify-center gap-2">
                            <button
                                disabled={page === 0}
                                onClick={() => setPage(p => p - 1)}
                                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-gray-50"
                            >
                                Önceki
                            </button>
                            <span className="px-3 py-1.5 text-sm text-gray-500">{page + 1} / {totalPages}</span>
                            <button
                                disabled={page >= totalPages - 1}
                                onClick={() => setPage(p => p + 1)}
                                className="px-3 py-1.5 text-sm border rounded-lg disabled:opacity-40 hover:bg-gray-50"
                            >
                                Sonraki
                            </button>
                        </div>
                    )}
                </>
            )}

            {/* Detail Modal */}
            {showDetail && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto">
                    <div className="bg-white rounded-xl w-full max-w-2xl my-6 p-6 space-y-5">
                        <div className="flex items-start justify-between">
                            <h2 className="text-lg font-semibold text-gray-900">Hasar Tespiti Detayı</h2>
                            <button onClick={() => setShowDetail(null)} className="text-gray-400 hover:text-gray-600">
                                <X className="h-5 w-5" />
                            </button>
                        </div>

                        {/* Lokasyon */}
                        <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                            <p className="font-medium text-gray-800">{showDetail.address}</p>
                            {showDetail.streetName && (
                                <p className="text-sm text-gray-500">
                                    {showDetail.streetName}{showDetail.buildingNo ? ` No: ${showDetail.buildingNo}` : ''}
                                </p>
                            )}
                            <p className="text-sm text-gray-500">{showDetail.neighborhoodName}, {showDetail.districtName}</p>
                            {showDetail.latitude && showDetail.longitude ? (
                                <a
                                    href={googleMapsUrl(showDetail.latitude, showDetail.longitude)}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="inline-flex items-center gap-1.5 text-sm text-blue-600 hover:text-blue-800 font-medium"
                                >
                                    <Navigation className="h-4 w-4" />
                                    Google Maps'te Göster
                                </a>
                            ) : null}
                        </div>

                        {/* Hasar Bilgileri */}
                        <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                                <p className="text-gray-500 mb-1">Hasar Düzeyi</p>
                                <span className={`px-2 py-0.5 rounded-full text-xs ${damageLevelColor(showDetail.damageLevel)}`}>
                                    {showDetail.damageLevelLabel}
                                </span>
                            </div>
                            <div>
                                <p className="text-gray-500 mb-1">Doğrulama Durumu</p>
                                <span className={`px-2 py-0.5 rounded-full text-xs ${verificationStatusColor(showDetail.verificationStatus)}`}>
                                    {showDetail.verificationStatusLabel}
                                </span>
                            </div>
                        </div>

                        {/* Acil Durum Bayrakları */}
                        {(showDetail.collapseRisk || showDetail.emergencyEvacuationNeeded ||
                          showDetail.casualtiesSuspected || showDetail.blockedRoad || showDetail.gasLeakRisk) && (
                            <div className="border border-red-100 bg-red-50 rounded-lg p-3">
                                <p className="text-xs font-semibold text-red-700 mb-2">Acil Durum Uyarıları</p>
                                <div className="flex flex-wrap gap-2">
                                    {showDetail.collapseRisk && (
                                        <span className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-800">Çökme Riski</span>
                                    )}
                                    {showDetail.emergencyEvacuationNeeded && (
                                        <span className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-800">Acil Tahliye</span>
                                    )}
                                    {showDetail.casualtiesSuspected && (
                                        <span className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-800">Kayıp/Yaralı Şüphesi</span>
                                    )}
                                    {showDetail.blockedRoad && (
                                        <span className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-800">Yol Kapandı</span>
                                    )}
                                    {showDetail.gasLeakRisk && (
                                        <span className="px-2 py-0.5 rounded-full text-xs bg-red-100 text-red-800">Gaz Kaçağı</span>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Kayıt Geçmişi */}
                        <div className="border-t border-gray-100 pt-4 space-y-2 text-sm">
                            <h3 className="font-medium text-gray-700 flex items-center gap-1.5">
                                <BadgeCheck className="h-4 w-4 text-gray-400" />
                                Kayıt Geçmişi
                            </h3>
                            <div className="space-y-1.5">
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Oluşturma Tarihi:</span>
                                    <span className="text-gray-800">{new Date(showDetail.createdAt).toLocaleString('tr-TR')}</span>
                                </div>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Son Güncelleme:</span>
                                    <span className="text-gray-800">{new Date(showDetail.updatedAt).toLocaleString('tr-TR')}</span>
                                </div>
                                {showDetail.verifiedBy && (
                                    <>
                                        <div className="flex gap-2">
                                            <span className="text-gray-500 w-36 flex-shrink-0">Sahada Doğrulayan:</span>
                                            <span className="text-gray-800">{showDetail.verifiedBy}</span>
                                        </div>
                                        {showDetail.verifiedAt && (
                                            <div className="flex gap-2">
                                                <span className="text-gray-500 w-36 flex-shrink-0">Doğrulama Tarihi:</span>
                                                <span className="text-gray-800">
                                                    {new Date(showDetail.verifiedAt).toLocaleString('tr-TR')}
                                                </span>
                                            </div>
                                        )}
                                    </>
                                )}
                                {showDetail.approvedBy && (
                                    <>
                                        <div className="flex gap-2">
                                            <span className="text-gray-500 w-36 flex-shrink-0">Koordinatör Onayı:</span>
                                            <span className="text-gray-800">{showDetail.approvedBy}</span>
                                        </div>
                                        {showDetail.approvedAt && (
                                            <div className="flex gap-2">
                                                <span className="text-gray-500 w-36 flex-shrink-0">Onay Tarihi:</span>
                                                <span className="text-gray-800">
                                                    {new Date(showDetail.approvedAt).toLocaleString('tr-TR')}
                                                </span>
                                            </div>
                                        )}
                                    </>
                                )}
                            </div>
                        </div>

                        {/* Not */}
                        {showDetail.note && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm text-gray-500 mb-1">Not</p>
                                <p className="text-sm text-gray-800">{showDetail.note}</p>
                            </div>
                        )}

                        {/* Fotoğraflar */}
                        {showDetail.photoUrls && showDetail.photoUrls.length > 0 && (
                            <div className="border-t border-gray-100 pt-4">
                                <p className="text-sm font-medium text-gray-700 mb-3">
                                    Fotoğraflar ({showDetail.photoUrls.length})
                                </p>
                                <div className="grid grid-cols-3 gap-2">
                                    {showDetail.photoUrls.map((url, i) => (
                                        <a key={i} href={url} target="_blank" rel="noopener noreferrer">
                                            <img
                                                src={url}
                                                alt={`Fotoğraf ${i + 1}`}
                                                className="w-full h-24 object-cover rounded-lg border border-gray-200 hover:opacity-90 transition-opacity"
                                            />
                                        </a>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="flex justify-end pt-2">
                            <button
                                onClick={() => setShowDetail(null)}
                                className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                            >
                                Kapat
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
