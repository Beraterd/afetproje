import React, { useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getDistricts, getNeighborhoodsByDistrict } from '@/api/districts.api';
import { useAuthStore } from '@/store/authStore';
import { Button } from '@/components/ui';
import { FileBarChart2, Lock } from 'lucide-react';

export interface ReportFilterValue {
    districtId?: string;
    neighborhoodId?: string;
    date?: string; // YYYY-MM-DD
}

interface Props {
    value: ReportFilterValue;
    onChange: (next: ReportFilterValue) => void;
    onGenerate: () => void;
    showDate?: boolean;
    periodHint?: string;
    loading?: boolean;
}

/**
 * Faz 2 — Rol bazlı ilçe/mahalle + tarih filtresi. Kilit mantığı:
 * Admin → serbest; İlçe Koord. → ilçe kilitli; Mahalle Koord. → ilçe + mahalle kilitli.
 * Asıl yetki backend'de zorlanır; bu kilitler kullanıcı deneyimi içindir.
 */
export const ReportFilterBar: React.FC<Props> = ({ value, onChange, onGenerate, showDate, periodHint, loading }) => {
    const user = useAuthStore((s) => s.user);
    const role = user?.role;
    const isAdmin = role === 'ADMIN';
    const isDistrictCoord = role === 'DISTRICT_COORDINATOR';
    const isNeighborhoodCoord = role === 'NEIGHBORHOOD_COORDINATOR';

    const districtLocked = isDistrictCoord || isNeighborhoodCoord;
    const neighborhoodLocked = isNeighborhoodCoord;

    const { data: districts = [] } = useQuery({
        queryKey: ['report', 'districts'],
        queryFn: getDistricts,
        staleTime: 5 * 60 * 1000,
    });

    // Koordinatörler için kendi kapsamlarını başlangıçta sabitle
    useEffect(() => {
        if (districtLocked && user?.districtId && value.districtId !== user.districtId) {
            onChange({
                ...value,
                districtId: user.districtId,
                neighborhoodId: isNeighborhoodCoord ? user.neighborhoodId : value.neighborhoodId,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [districtLocked, user?.districtId]);

    const effectiveDistrictId = districtLocked ? user?.districtId : value.districtId;

    const { data: neighborhoods = [] } = useQuery({
        queryKey: ['report', 'neighborhoods', effectiveDistrictId],
        queryFn: () => getNeighborhoodsByDistrict(effectiveDistrictId as string),
        enabled: !!effectiveDistrictId,
        staleTime: 5 * 60 * 1000,
    });

    const lockedDistrictName = useMemo(
        () => districts.find((d) => d.id === user?.districtId)?.name ?? 'Atanmış ilçe',
        [districts, user?.districtId],
    );
    const lockedNeighborhoodName = useMemo(
        () => neighborhoods.find((n) => n.id === user?.neighborhoodId)?.name ?? 'Atanmış mahalle',
        [neighborhoods, user?.neighborhoodId],
    );

    return (
        <div className="glass-card px-4 py-3 no-print">
            <div className="flex flex-wrap items-end gap-3">
                {/* İlçe */}
                <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-gray-600 flex items-center gap-1">
                        İlçe {districtLocked && <Lock className="h-3 w-3 text-gray-400" />}
                    </label>
                    {districtLocked ? (
                        <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-gray-700 min-w-[10rem]">
                            {lockedDistrictName}
                        </div>
                    ) : (
                        <select
                            value={value.districtId ?? ''}
                            onChange={(e) => onChange({ ...value, districtId: e.target.value || undefined, neighborhoodId: undefined })}
                            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:ring-brand-500 min-w-[10rem]"
                        >
                            <option value="">Tüm İlçeler (Sistem)</option>
                            {districts.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
                        </select>
                    )}
                </div>

                {/* Mahalle */}
                <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-gray-600 flex items-center gap-1">
                        Mahalle {neighborhoodLocked && <Lock className="h-3 w-3 text-gray-400" />}
                    </label>
                    {neighborhoodLocked ? (
                        <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-gray-700 min-w-[10rem]">
                            {lockedNeighborhoodName}
                        </div>
                    ) : (
                        <select
                            value={value.neighborhoodId ?? ''}
                            onChange={(e) => onChange({ ...value, neighborhoodId: e.target.value || undefined })}
                            disabled={!effectiveDistrictId}
                            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:ring-brand-500 disabled:opacity-50 min-w-[10rem]"
                        >
                            <option value="">{effectiveDistrictId ? 'Tüm Mahalleler' : 'Önce ilçe seçin'}</option>
                            {neighborhoods.map((n) => <option key={n.id} value={n.id}>{n.name}</option>)}
                        </select>
                    )}
                </div>

                {/* Tarih / Dönem */}
                <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-gray-600">Tarih / Dönem</label>
                    {showDate ? (
                        <input
                            type="date"
                            value={value.date ?? ''}
                            onChange={(e) => onChange({ ...value, date: e.target.value || undefined })}
                            className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-brand-500 focus:ring-brand-500"
                        />
                    ) : (
                        <div className="rounded-lg border border-gray-200 bg-gray-50 px-3 py-1.5 text-sm text-gray-500 min-w-[12rem]">
                            {periodHint ?? 'Otomatik dönem'}
                        </div>
                    )}
                </div>

                <Button
                    variant="primary"
                    onClick={onGenerate}
                    loading={loading}
                    leftIcon={<FileBarChart2 className="h-4 w-4" />}
                >
                    Rapor Oluştur
                </Button>
            </div>
        </div>
    );
};
