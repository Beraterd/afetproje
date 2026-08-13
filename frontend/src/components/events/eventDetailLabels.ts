/** EventDetailPage'de paylaşılan saf etiket yardımcıları — API çağrısı yok. */

export const TEAM_TR: Record<string, string> = {
    SEARCH_RESCUE: 'Arama Kurtarma Ekibi',
    FOOD_WATER: 'Yemek ve İçme Suyu Dağıtım Ekibi',
    EVACUATION: 'Tahliye Ekibi',
    COMMUNICATION: 'İletişim Ekibi',
    PSYCHOSOCIAL: 'Psikososyal Destek Ekibi',
    HASAR_TESPIT_EKIBI: 'Hasar Tespit Ekibi',
    OTHER: 'Diğer',
};

export const PROXIMITY_TR: Record<string, string> = {
    SAME_NEIGHBORHOOD: 'Aynı mahalle',
    SAME_DISTRICT: 'Aynı ilçe',
    NEIGHBOR_DISTRICT: 'Komşu ilçe',
    ISTANBUL_WIDE: 'İstanbul geneli',
    NONE: '-',
};

export const AVAILABILITY_TR: Record<string, string> = {
    AVAILABLE: 'Müsait',
    ACTIVE_TASK: 'Aktif görevde',
    UNAVAILABLE: 'Müsait değil',
};

export const PARTICIPANT_STATUS_TR: Record<string, { label: string; color: string }> = {
    JOINED:    { label: 'Katıldı',       color: 'text-green-600 bg-green-50' },
    ACCEPTED:  { label: 'Kabul Etti',    color: 'text-green-600 bg-green-50' },
    INVITED:   { label: 'Davet Edildi',  color: 'text-blue-600 bg-blue-50' },
    DECLINED:  { label: 'Reddetti',      color: 'text-red-600 bg-red-50' },
    CANCELLED: { label: 'İptal',         color: 'text-gray-500 bg-gray-50' },
};

// Bu mesajlar "zaten katıldınız" anlamına gelir → hata değil, info toast + refresh
const ALREADY_JOINED_MSGS = [
    'zaten katılıyorsunuz',
    'davet yoluyla zaten kabul',
    'already joined',
];

export function isAlreadyJoinedError(msg: string): boolean {
    const lower = msg?.toLowerCase() ?? '';
    return ALREADY_JOINED_MSGS.some((s) => lower.includes(s));
}
