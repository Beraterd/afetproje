/** Hasar tespiti ekranlarında paylaşılan saf sunum yardımcıları — API çağrısı yok. */

export const ACCEPTED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

export function googleMapsUrl(lat: number, lng: number): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
}

export function damageLevelColor(level: string): string {
    switch (level) {
        case 'LIGHT': return 'bg-yellow-100 text-yellow-800';
        case 'MODERATE': return 'bg-orange-100 text-orange-800';
        case 'HEAVY': return 'bg-red-100 text-red-800';
        case 'COLLAPSED': return 'bg-red-200 text-red-900 font-bold';
        default: return 'bg-gray-100 text-gray-600';
    }
}

export function verificationStatusColor(status: string): string {
    switch (status) {
        case 'INCELEME_GEREKIYOR': return 'bg-yellow-100 text-yellow-800';
        case 'ASSIGNED': return 'bg-purple-100 text-purple-800';
        case 'SAHADA_DOGRULANDI': return 'bg-blue-100 text-blue-800';
        case 'KOORDINATOR_ONAYLADI': return 'bg-green-100 text-green-800';
        default: return 'bg-gray-100 text-gray-600';
    }
}

export function aiConfidenceBadgeColor(confidence: string): string {
    switch (confidence) {
        case 'HIGH': return 'bg-red-100 text-red-800';
        case 'MEDIUM': return 'bg-orange-100 text-orange-800';
        case 'LOW': return 'bg-yellow-100 text-yellow-800';
        default: return 'bg-gray-100 text-gray-600';
    }
}
