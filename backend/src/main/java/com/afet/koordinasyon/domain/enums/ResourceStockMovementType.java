package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Stok hareketi türü. Her stok miktarı değişiminde bir hareket kaydı oluşur.
 */
@Getter
@RequiredArgsConstructor
public enum ResourceStockMovementType {
    INITIAL("İlk kayıt"),
    INCREASE("Giriş"),
    DECREASE("Çıkış"),
    CORRECTION("Düzeltme"),
    RESERVED("Rezerve"),
    RELEASED("Rezerv iadesi");

    private final String label;
}
