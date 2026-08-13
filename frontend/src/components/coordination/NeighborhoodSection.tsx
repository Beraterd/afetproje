import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle, MapPin } from 'lucide-react';
import { LoadingSpinner } from '@/components/ui';
import { useToast } from '@/components/shared/ToastProvider';
import { queryKeys } from '@/utils/queryKeys';
import { getNeighborhoodCenter, upsertNeighborhoodCenter } from '@/api/coordinationCenters.api';
import { getApiErrorMessage } from '@/utils/errorParser';
import type { MapNeighborhoodResponse } from '@/types';
import type { UpsertCoordinationCenterRequest } from '@/types/coordinationCenter';
import { CenterForm } from './CenterForm';

/** Bir mahallenin koordinasyon merkezi kaydını çeker/kaydeder — bkz. DistrictSection. */
export const NeighborhoodSection: React.FC<{
    neighborhoodId: string;
    neighborhood?: MapNeighborhoodResponse;
    districtName?: string;
}> = ({ neighborhoodId, neighborhood, districtName }) => {
    const queryClient = useQueryClient();
    const { success, error } = useToast();

    const { data: center, isLoading } = useQuery({
        queryKey: queryKeys.coordinationCenters.neighborhood(neighborhoodId),
        queryFn: () => getNeighborhoodCenter(neighborhoodId),
        retry: (failCount, err: any) => err?.status !== 404 && failCount < 2,
    });

    const { mutateAsync: save, isPending: saving } = useMutation({
        mutationFn: (req: UpsertCoordinationCenterRequest) => upsertNeighborhoodCenter(neighborhoodId, req),
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: queryKeys.coordinationCenters.neighborhood(neighborhoodId),
            });
            queryClient.invalidateQueries({ queryKey: queryKeys.coordinationCenters.myStatus() });
            success('Koordinatörlük merkezi başarıyla kaydedildi.');
        },
        onError: (err: any) => {
            error(getApiErrorMessage(err, 'Kaydetme sırasında bir hata oluştu.'));
        },
    });

    if (isLoading) {
        return (
            <div className="flex justify-center py-12">
                <LoadingSpinner size="lg" />
            </div>
        );
    }

    return (
        <div>
            {center ? (
                <div className="flex items-center gap-2 mb-4 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg px-4 py-2.5">
                    <CheckCircle className="h-4 w-4 flex-shrink-0" />
                    <span>Merkez tanımlı — güncelleme yapabilirsiniz.</span>
                </div>
            ) : (
                <div className="flex items-center gap-2 mb-4 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-4 py-2.5">
                    <MapPin className="h-4 w-4 flex-shrink-0" />
                    <span>Bu mahalle için henüz koordinatörlük merkezi tanımlanmamış.</span>
                </div>
            )}
            <CenterForm
                polygon={neighborhood?.polygon ?? null}
                boundaryLabel="mahalle"
                areaName={neighborhood?.name}
                districtName={districtName}
                existingCenter={center}
                onSave={save}
                saving={saving}
            />
        </div>
    );
};
