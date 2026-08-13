import { useQuery } from '@tanstack/react-query';
import { X, Building2 } from 'lucide-react';
import { LoadingSpinner, Badge } from '@/components/ui';
import { DamagePhotoGallery } from '@/components/damage/DamagePhotoGallery';
import { getBuildingById } from '@/api/buildings.api';
import { queryKeys } from '@/utils/queryKeys';

const DAMAGE_BADGE_VARIANT: Record<string, 'neutral' | 'warning' | 'high' | 'danger'> = {
    LIGHT: 'warning',
    MODERATE: 'high',
    HEAVY: 'danger',
    COLLAPSED: 'danger',
    UNASSESSED: 'neutral',
};

function InfoRow({ label, value }: { label: string; value: string }) {
    return (
        <div className="flex justify-between gap-3 text-sm py-1 border-b border-slate-100 last:border-0">
            <span className="text-slate-500">{label}</span>
            <span className="text-slate-900 font-medium text-right">{value}</span>
        </div>
    );
}

interface BuildingDetailsPanelProps {
    buildingId: string;
    onClose: () => void;
}

/** Seçilen binanın tam detayı — kendi verisini kendi çeker (self-contained, CoordinationCenterPage'in
 *  bölümleri gibi disiplinli bir istisna). Operasyon Haritası'nda ve debug amaçlı Map3DPage'de
 *  kullanılır. Gerçek veri yoksa sayı UYDURMAZ — backend zaten "rapor yok" ile "değerlendirilmedi"yi
 *  ayırt ediyor (damageStatus null vs "UNASSESSED"), bu panel bu ayrımı olduğu gibi yansıtır.
 *  Masaüstünde sağda kompakt kart, mobilde alttan açılan sheet (MapEntityDetails ile aynı desen). */
export const BuildingDetailsPanel: React.FC<BuildingDetailsPanelProps> = ({ buildingId, onClose }) => {
    const query = useQuery({
        queryKey: queryKeys.buildings.detail(buildingId),
        queryFn: () => getBuildingById(buildingId),
    });

    return (
        <div
            className="absolute z-[1000] left-1/2 -translate-x-1/2 bottom-3 w-[calc(100%-24px)] max-w-sm
                       sm:left-auto sm:right-4 sm:top-4 sm:translate-x-0 sm:bottom-auto sm:w-80 sm:max-w-none"
        >
            <div className="bg-white rounded-lg shadow-xl border border-slate-200 p-4 max-h-[60vh] sm:max-h-[calc(100vh-140px)] overflow-y-auto">
                <div className="flex items-start justify-between mb-2">
                    <h2 className="font-semibold text-slate-900 text-sm flex items-center gap-1.5">
                        <Building2 size={15} className="text-slate-400" /> Bina Bilgisi
                    </h2>
                    <button onClick={onClose} aria-label="Kapat" className="text-slate-400 hover:text-slate-700">
                        <X size={16} />
                    </button>
                </div>

                {query.isLoading && (
                    <div className="py-6 flex justify-center"><LoadingSpinner size="sm" /></div>
                )}
                {query.isError && (
                    <p className="text-sm text-red-600 py-2">Bina detayı yüklenemedi.</p>
                )}
                {query.data && (
                    <div className="space-y-3">
                        {query.data.damageStatus ? (
                            <Badge variant={DAMAGE_BADGE_VARIANT[query.data.damageStatus] ?? 'neutral'}>
                                {query.data.damageStatusLabel}
                            </Badge>
                        ) : (
                            <Badge variant="neutral">Rapor yok</Badge>
                        )}

                        <div className="space-y-0.5">
                            <InfoRow label="Adres" value={query.data.displayAddress ?? 'Veri bulunmuyor'} />
                            <InfoRow label="Mahalle" value={query.data.neighborhoodName} />
                            <InfoRow label="Bina tipi" value={query.data.buildingType ?? 'Veri bulunmuyor'} />
                            <InfoRow
                                label="Kat sayısı"
                                value={query.data.levels != null ? String(query.data.levels) : 'Veri bulunmuyor'}
                            />
                            <InfoRow
                                label="Yükseklik"
                                value={
                                    query.data.isEstimatedHeight
                                        ? `~${query.data.estimatedHeightM} m (tahmini)`
                                        : `${query.data.estimatedHeightM} m (OSM)`
                                }
                            />
                            <InfoRow
                                label="Hasar kaydı"
                                value={`${query.data.damageAssessmentCount} bildirim, ${query.data.verifiedAssessmentCount} saha doğrulaması`}
                            />
                        </div>

                        {query.data.latestAssessment && (
                            <div className="border-t border-slate-100 pt-3 space-y-2">
                                <p className="text-xs font-semibold text-slate-700">Son değerlendirme</p>
                                <div className="space-y-0.5">
                                    <InfoRow label="Doğrulama durumu" value={query.data.latestAssessment.verificationStatusLabel} />
                                    <InfoRow
                                        label="Tarih"
                                        value={new Date(query.data.latestAssessment.createdAt).toLocaleString('tr-TR')}
                                    />
                                </div>
                                <DamagePhotoGallery
                                    title="Fotoğraflar"
                                    photoUrls={query.data.latestAssessment.photoUrls.slice(0, 3)}
                                    altPrefix="Bina hasar fotoğrafı"
                                />
                                {query.data.latestAssessment.photoUrls.length > 3 && (
                                    <p className="text-xs text-slate-400">
                                        +{query.data.latestAssessment.photoUrls.length - 3} fotoğraf daha
                                    </p>
                                )}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};
