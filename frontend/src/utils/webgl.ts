/** Bu tarayıcı/cihazda WebGL destekleniyor mu — maplibre-gl'in kendisini import etmeden, ucuz bir
 *  canvas probe ile. `BuildingMap`'i mount etmeden ÖNCE çağrılmalı (WebGL yoksa maplibre-gl kurulum
 *  sırasında sessizce/gürültülü hata verebilir). */
export function isWebglSupported(): boolean {
    try {
        const canvas = document.createElement('canvas');
        return !!(canvas.getContext('webgl2') || canvas.getContext('webgl') || canvas.getContext('experimental-webgl'));
    } catch {
        return false;
    }
}
