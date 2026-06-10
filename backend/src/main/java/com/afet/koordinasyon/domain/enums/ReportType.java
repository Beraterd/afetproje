package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Rapor Merkezi'ndeki rapor türleri.
 * DAILY/WEEKLY/MONTHLY kapsamlı (tüm modüller) raporlardır; diğerleri tek konuya odaklı özel raporlardır.
 * ("İlçe Raporu" ve "Mahalle Raporu" ayrı tip değildir — tüm raporlar ilçe/mahalle filtresiyle çalışır.)
 */
@Getter
@RequiredArgsConstructor
public enum ReportType {
    DAILY("Günlük Rapor"),
    WEEKLY("Haftalık Rapor"),
    MONTHLY("Aylık Rapor"),
    OPERATION("Ekip Raporu"),
    RESOURCE("Kaynak Talep Raporu"),
    STOCK("Stok Raporu"),
    DAMAGE("Hasar Raporu"),
    VOLUNTEER("Gönüllü Raporu");

    private final String label;
}
