package com.afet.koordinasyon.domain.enums;

/**
 * Deprem bildirim kaynağı. Gerçek AFAD depremi ile admin tarafından
 * tetiklenen simülasyonu ayırt etmek için kullanılır.
 * <p>
 * Kullanıcıya gönderilen bilgi yapısı her iki kaynak için aynıdır; bu ayrım
 * yalnızca başlık/log/rapor tarafında kullanılır.
 */
public enum EarthquakeSourceType {
    REAL,
    SIMULATION
}
