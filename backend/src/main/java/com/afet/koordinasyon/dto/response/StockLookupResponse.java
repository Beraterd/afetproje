package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Talep açma modalında gösterilen, belirli bölge + kaynak türü için stok özeti.
 */
@Data
@Builder
public class StockLookupResponse {
    /** Bu bölge + türde stok kaydı var mı. */
    private boolean hasStock;
    /** Toplam mevcut miktar. */
    private int totalQuantity;
    private String unit;
    /** Birleşik yeterlilik durumu (en kötü durum). */
    private String status;
    private String statusLabel;
    /** Ortalama yeterlilik günü; hesaplanamıyorsa null. */
    private Double daysRemaining;
    /** Kullanıcıya gösterilecek hazır Türkçe mesaj. */
    private String message;
}
