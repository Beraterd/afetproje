/** Türkçe alfabetik karşılaştırma — Ç/Ğ/İ/I/ı/Ö/Ş/Ü harflerini `localeCompare('tr')`
 *  ile doğru sıralar (basit ASCII/kod-noktası sıralaması bunları yanlış sıralar,
 *  ör. "Çekmeköy" ASCII'de "Z"den sonra gelir). Yalnızca sunum sırasını belirler —
 *  seçili district/neighborhood ID'lerini veya URL state'ini etkilemez. */
export function turkishCompare(a: string, b: string): number {
    return a.localeCompare(b, 'tr');
}

/** `name` alanına göre Türkçe alfabetik sıralı YENİ bir dizi döner (orijinali mutasyona
 *  uğratmaz — API'den gelen query-cache verisini yerinde sıralamak react-query'nin
 *  referans eşitliği varsayımlarını bozabilir). */
export function sortByNameTr<T extends { name: string }>(items: readonly T[]): T[] {
    return [...items].sort((a, b) => turkishCompare(a.name, b.name));
}
