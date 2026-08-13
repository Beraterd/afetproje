import type { CoordinationCenterResponse } from '@/types/coordinationCenter';

export interface CenterFormState {
    address: string;
    streetName: string;
    buildingNo: string;
    lat: number | null;
    lng: number | null;
    locationConfirmed: boolean;
}

export const defaultCenterForm = (): CenterFormState => ({
    address: '',
    streetName: '',
    buildingNo: '',
    lat: null,
    lng: null,
    locationConfirmed: false,
});

export function centerFormFromResponse(center: CoordinationCenterResponse): CenterFormState {
    return {
        address: center.address,
        streetName: center.streetName ?? '',
        buildingNo: center.buildingNo ?? '',
        lat: center.latitude,
        lng: center.longitude,
        locationConfirmed: center.locationVerified,
    };
}
