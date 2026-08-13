import React from 'react';
import { Badge } from '@/components/ui';
import { Navigation, Camera, X, CheckCircle, MapPin, Image, Brain } from 'lucide-react';
import { AuthenticatedImage } from '@/components/shared/AuthenticatedImage';
import type { DamageAssessmentTaskResponse } from '@/types';
import { DAMAGE_LEVEL_COLOR, ASSIGNMENT_STATUS_VARIANT, mapsUrl } from './taskLabels';

interface DamageTaskRowProps {
    task: DamageAssessmentTaskResponse;
    photoPanel: string | null;
    selectedPhotos: File[];
    photoErrors: string[];
    uploading: boolean;
    actionLoading: boolean;
    photoInputRef: React.RefObject<HTMLInputElement>;
    onOpenPhotoPanel: (id: string) => void;
    onClosePhotoPanel: () => void;
    onRemovePhoto: (i: number) => void;
    onUpload: () => void;
    onFieldVerify: (id: string) => void;
    onComplete: (id: string) => void;
}

/** Aktif hasar tespiti görev satırı — konum/AI özeti, aksiyon butonları ve (açıksa) saha
 *  fotoğrafı yükleme paneli. Tüm API çağrıları üst container'da (MyTasksPage) kalır. */
export const DamageTaskRow: React.FC<DamageTaskRowProps> = ({
    task, photoPanel, selectedPhotos, photoErrors, uploading, actionLoading,
    photoInputRef, onOpenPhotoPanel, onClosePhotoPanel,
    onRemovePhoto, onUpload, onFieldVerify, onComplete,
}) => {
    const isPanelOpen = photoPanel === task.assignmentId;
    const canVerify = task.assignmentStatus === 'ACTIVE';
    const canComplete = task.assignmentStatus === 'ACTIVE' || task.assignmentStatus === 'FIELD_VERIFIED';

    const urgencyFlags = [
        task.collapseRisk && 'Çökme Riski',
        task.emergencyEvacuationNeeded && 'Acil Tahliye',
        task.casualtiesSuspected && 'Kayıp/Yaralı',
        task.gasLeakRisk && 'Gaz Kaçağı',
    ].filter(Boolean) as string[];

    return (
        <div className="px-4 py-4 sm:px-6 space-y-3">
            {/* Header row */}
            <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-900 flex items-center gap-1.5">
                        <MapPin className="h-3.5 w-3.5 text-orange-500 flex-shrink-0" />
                        <span className="truncate">{task.address}</span>
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5">
                        {task.districtName} / {task.neighborhoodName}
                    </p>
                    {urgencyFlags.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-1">
                            {urgencyFlags.map(f => (
                                <span key={f} className="text-xs bg-red-50 text-red-700 rounded px-1.5 py-0.5">{f}</span>
                            ))}
                        </div>
                    )}
                    <p className="text-xs text-gray-400 mt-1">
                        Atayan: {task.assignedByName} · {new Date(task.assignedAt).toLocaleDateString('tr-TR')}
                    </p>
                </div>
                <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${DAMAGE_LEVEL_COLOR[task.damageLevel] || 'bg-gray-100 text-gray-600'}`}>
                        {task.damageLevelLabel}
                    </span>
                    <Badge variant={ASSIGNMENT_STATUS_VARIANT[task.assignmentStatus] || 'neutral'}>
                        {task.assignmentStatusLabel}
                    </Badge>
                </div>
            </div>

            {/* AI Ön Değerlendirme */}
            {task.aiAnalysisStatus === 'PENDING' && (
                <div className="bg-purple-50 rounded-lg px-3 py-2 flex items-center gap-2">
                    <Brain className="h-4 w-4 text-purple-500 animate-pulse flex-shrink-0" />
                    <p className="text-xs text-purple-700">Yapay zeka analiz ediliyor...</p>
                </div>
            )}
            {task.aiAnalysisStatus === 'COMPLETED' && task.aiComment && (
                <div className="space-y-2">
                    <div className="bg-purple-50 rounded-lg p-3 space-y-1.5">
                        <div className="flex items-center gap-1.5">
                            <Brain className="h-3.5 w-3.5 text-purple-500 flex-shrink-0" />
                            <span className="text-xs font-medium text-purple-700">Yapay Zeka Ön Değerlendirmesi</span>
                            {task.aiConfidence && (
                                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ml-auto ${
                                    task.aiConfidence === 'HIGH' ? 'bg-red-100 text-red-800' :
                                    task.aiConfidence === 'MEDIUM' ? 'bg-orange-100 text-orange-800' :
                                    'bg-yellow-100 text-yellow-800'
                                }`}>
                                    {task.aiConfidenceLabel || task.aiConfidence}
                                </span>
                            )}
                        </div>
                        <p className="text-xs text-gray-700">{task.aiComment}</p>
                    </div>
                    <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-1.5 flex items-start gap-1.5">
                        <span className="text-amber-500 text-xs flex-shrink-0">⚠</span>
                        <p className="text-xs text-amber-700">
                            Bu yorum yapay zekâ ön değerlendirmesidir. Resmi saha incelemesi yerine geçmez.
                        </p>
                    </div>
                </div>
            )}

            {/* Action buttons */}
            <div className="flex flex-wrap gap-2">
                <a
                    href={mapsUrl(task.latitude, task.longitude, task.address)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-blue-300 rounded-lg text-xs font-medium text-blue-700 hover:bg-blue-50 transition-colors"
                >
                    <Navigation className="h-3.5 w-3.5" />
                    Konuma Git
                </a>
                <button
                    onClick={() => isPanelOpen ? onClosePhotoPanel() : onOpenPhotoPanel(task.assignmentId)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-gray-300 rounded-lg text-xs font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                >
                    <Camera className="h-3.5 w-3.5" />
                    Fotoğraf Yükle
                    {task.fieldPhotoUrls.length > 0 && (
                        <span className="bg-gray-200 text-gray-600 rounded-full px-1.5 py-0.5 text-xs">
                            {task.fieldPhotoUrls.length}
                        </span>
                    )}
                </button>
                {canVerify && (
                    <button
                        onClick={() => onFieldVerify(task.assignmentId)}
                        disabled={actionLoading}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-medium disabled:opacity-50 transition-colors"
                    >
                        <CheckCircle className="h-3.5 w-3.5" />
                        Sahada Doğruladım
                    </button>
                )}
                {canComplete && (
                    <button
                        onClick={() => onComplete(task.assignmentId)}
                        disabled={actionLoading}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded-lg text-xs font-medium disabled:opacity-50 transition-colors"
                    >
                        Görevi Tamamla
                    </button>
                )}
            </div>

            {/* Photo upload panel */}
            {isPanelOpen && (
                <div className="border border-gray-200 rounded-lg p-3 bg-gray-50 space-y-3">
                    <div className="flex items-center justify-between">
                        <p className="text-xs font-medium text-gray-700">Saha Fotoğrafı Yükle</p>
                        <button onClick={onClosePhotoPanel} aria-label="Fotoğraf panelini kapat" className="text-gray-400 hover:text-gray-600">
                            <X className="h-4 w-4" />
                        </button>
                    </div>

                    <button
                        type="button"
                        onClick={() => photoInputRef.current?.click()}
                        className="flex items-center gap-2 border-2 border-dashed border-gray-300 hover:border-orange-400 rounded-lg px-4 py-3 text-xs text-gray-500 hover:text-orange-600 transition-colors w-full justify-center"
                    >
                        <Image className="h-4 w-4" />
                        Fotoğraf Seç
                    </button>

                    {photoErrors.length > 0 && (
                        <div className="space-y-1">
                            {photoErrors.map((e, i) => (
                                <p key={i} className="text-xs text-red-600">{e}</p>
                            ))}
                        </div>
                    )}

                    {selectedPhotos.length > 0 && (
                        <div className="grid grid-cols-3 gap-2">
                            {selectedPhotos.map((photo, i) => (
                                <div key={i} className="relative group">
                                    <img
                                        src={URL.createObjectURL(photo)}
                                        alt={photo.name}
                                        className="w-full h-20 object-cover rounded-lg border border-gray-200"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => onRemovePhoto(i)}
                                        aria-label="Fotoğrafı kaldır"
                                        className="absolute -top-1.5 -right-1.5 bg-red-500 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                                    >
                                        <X className="h-3 w-3" />
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}

                    {selectedPhotos.length > 0 && (
                        <button
                            onClick={onUpload}
                            disabled={uploading}
                            className="w-full px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-lg text-xs font-medium disabled:opacity-60 transition-colors"
                        >
                            {uploading ? 'Yükleniyor...' : `${selectedPhotos.length} Fotoğraf Yükle`}
                        </button>
                    )}

                    {task.reporterPhotoUrls.length > 0 && (
                        <div>
                            <p className="text-xs text-gray-500 mb-2">Bildirim fotoğrafları ({task.reporterPhotoUrls.length})</p>
                            <div className="grid grid-cols-4 gap-1.5">
                                {task.reporterPhotoUrls.map((url, i) => (
                                    <AuthenticatedImage
                                        key={i}
                                        photoUrl={url}
                                        alt={`Bildirim fotoğrafı ${i + 1}`}
                                        className="w-full h-16 object-cover rounded border border-gray-200"
                                    />
                                ))}
                            </div>
                        </div>
                    )}

                    {task.fieldPhotoUrls.length > 0 && (
                        <div>
                            <p className="text-xs text-gray-500 mb-2">Saha fotoğrafları ({task.fieldPhotoUrls.length})</p>
                            <div className="grid grid-cols-4 gap-1.5">
                                {task.fieldPhotoUrls.map((url, i) => (
                                    <AuthenticatedImage
                                        key={i}
                                        photoUrl={url}
                                        alt={`Saha fotoğrafı ${i + 1}`}
                                        className="w-full h-16 object-cover rounded border border-gray-200"
                                    />
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};
