import React from 'react';
import { X, Navigation, UserPlus } from 'lucide-react';
import type { DamageAssessmentResponse } from '@/types';
import { googleMapsUrl, damageLevelColor, verificationStatusColor } from './damageDisplay';
import { DamagePhotoGallery } from './DamagePhotoGallery';
import { DamageAiAnalysis } from './DamageAiAnalysis';

interface DamageAssessmentDetailProps {
    assessment: DamageAssessmentResponse;
    onClose: () => void;
    canRefreshAi: boolean;
    aiRefreshing: boolean;
    onAiRefresh: (assessmentId: string) => void;
}

/** Hasar tespiti detay modalı — adres/konum, geçmiş, atanan görevliler, fotoğraflar ve AI
 *  değerlendirmesini bir araya getiren orkestrasyon; kendisi API çağrısı yapmaz. */
export const DamageAssessmentDetail: React.FC<DamageAssessmentDetailProps> = ({
    assessment, onClose, canRefreshAi, aiRefreshing, onAiRefresh,
}) => {
    const showFallbackPhotos = !assessment.reporterPhotoUrls && !assessment.fieldPhotoUrls
        && assessment.photoUrls && assessment.photoUrls.length > 0;

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto">
            <div className="bg-white rounded-xl w-full max-w-2xl my-6 p-6 space-y-5">
                <div className="flex items-start justify-between">
                    <h2 className="text-lg font-semibold text-gray-900">Hasar Tespiti Detayı</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* Adres + Konum */}
                <div className="bg-gray-50 rounded-lg p-4 space-y-2">
                    <p className="font-medium text-gray-800">{assessment.address}</p>
                    <p className="text-sm text-gray-500">{assessment.neighborhoodName}, {assessment.districtName}</p>
                    {assessment.latitude && assessment.longitude ? (
                        <a
                            href={googleMapsUrl(assessment.latitude, assessment.longitude)}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 text-sm text-blue-600 hover:text-blue-800 font-medium"
                        >
                            <Navigation className="h-4 w-4" />
                            Google Maps'te Yol Tarifi Al
                        </a>
                    ) : (
                        <span className="text-xs text-gray-400">Koordinat bilgisi yok</span>
                    )}
                </div>

                {/* Hasar Bilgileri */}
                <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                        <p className="text-gray-500 mb-1">Hasar Düzeyi</p>
                        <span className={`px-2 py-0.5 rounded-full text-xs ${damageLevelColor(assessment.damageLevel)}`}>
                            {assessment.damageLevelLabel}
                        </span>
                    </div>
                    <div>
                        <p className="text-gray-500 mb-1">Doğrulama Durumu</p>
                        <span className={`px-2 py-0.5 rounded-full text-xs ${verificationStatusColor(assessment.verificationStatus)}`}>
                            {assessment.verificationStatusLabel}
                        </span>
                    </div>
                </div>

                {/* Kayıt Geçmişi */}
                <div className="border-t border-gray-100 pt-4 space-y-3 text-sm">
                    <h3 className="font-medium text-gray-700">Kayıt Geçmişi</h3>
                    <div className="space-y-2">
                        <div className="flex gap-2">
                            <span className="text-gray-500 w-36 flex-shrink-0">Oluşturan:</span>
                            <span className="text-gray-800">{assessment.reportedBy || '-'}</span>
                        </div>
                        <div className="flex gap-2">
                            <span className="text-gray-500 w-36 flex-shrink-0">Oluşturma Tarihi:</span>
                            <span className="text-gray-800">{new Date(assessment.createdAt).toLocaleString('tr-TR')}</span>
                        </div>
                        {assessment.verifiedBy && (
                            <>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Sahada Doğrulayan:</span>
                                    <span className="text-gray-800">{assessment.verifiedBy}</span>
                                </div>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Doğrulama Tarihi:</span>
                                    <span className="text-gray-800">{assessment.verifiedAt ? new Date(assessment.verifiedAt).toLocaleString('tr-TR') : '-'}</span>
                                </div>
                            </>
                        )}
                        {assessment.approvedBy && (
                            <>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Koordinatör Onayı:</span>
                                    <span className="text-gray-800">{assessment.approvedBy}</span>
                                </div>
                                <div className="flex gap-2">
                                    <span className="text-gray-500 w-36 flex-shrink-0">Onay Tarihi:</span>
                                    <span className="text-gray-800">{assessment.approvedAt ? new Date(assessment.approvedAt).toLocaleString('tr-TR') : '-'}</span>
                                </div>
                            </>
                        )}
                    </div>
                </div>

                {/* Atanan Görevliler */}
                {assessment.assignments && assessment.assignments.length > 0 && (
                    <div className="border-t border-gray-100 pt-4">
                        <p className="text-sm font-medium text-gray-700 mb-2">Atanan Görevliler</p>
                        <div className="space-y-1">
                            {assessment.assignments.map((a) => (
                                <div key={a.id} className="flex items-center gap-2 text-sm bg-purple-50 rounded-lg px-3 py-1.5">
                                    <UserPlus className="h-3.5 w-3.5 text-purple-500 flex-shrink-0" />
                                    <span className="font-medium text-gray-800">{a.firstName} {a.lastName}</span>
                                    <span className="text-gray-400">·</span>
                                    <span className="text-gray-500 text-xs">{a.email}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Not */}
                {assessment.note && (
                    <div className="border-t border-gray-100 pt-4">
                        <p className="text-sm text-gray-500 mb-1">Not</p>
                        <p className="text-sm text-gray-800">{assessment.note}</p>
                    </div>
                )}

                <DamagePhotoGallery
                    title="Bildirim Fotoğrafları"
                    photoUrls={assessment.reporterPhotoUrls ?? []}
                    altPrefix="Bildirim fotoğrafı"
                />
                <DamagePhotoGallery
                    title="Saha Fotoğrafları"
                    photoUrls={assessment.fieldPhotoUrls ?? []}
                    altPrefix="Saha fotoğrafı"
                />
                {showFallbackPhotos && (
                    <DamagePhotoGallery
                        title="Fotoğraflar"
                        photoUrls={assessment.photoUrls ?? []}
                        altPrefix="Fotoğraf"
                    />
                )}

                <DamageAiAnalysis
                    assessment={assessment}
                    canRefresh={canRefreshAi}
                    refreshing={aiRefreshing}
                    onRefresh={() => onAiRefresh(assessment.id)}
                />

                <div className="flex justify-end pt-2">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50"
                    >
                        Kapat
                    </button>
                </div>
            </div>
        </div>
    );
};
