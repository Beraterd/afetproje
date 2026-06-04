package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stok yeterlilik durumu. Kalıcı saklanmaz; miktar / kritik eşik / günlük tüketim
 * bilgisinden servis katmanında hesaplanır.
 */
@Getter
@RequiredArgsConstructor
public enum ResourceStockStatus {
    SUFFICIENT("Yeterli"),
    DECREASING("Azalıyor"),
    CRITICAL("Kritik"),
    OUT_OF_STOCK("Tükendi");

    private final String label;
}
