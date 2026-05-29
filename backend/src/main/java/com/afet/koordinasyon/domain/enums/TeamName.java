package com.afet.koordinasyon.domain.enums;

public enum TeamName {
    SEARCH_RESCUE("Arama Kurtarma Ekibi"),
    FOOD_WATER("Yemek ve İçme Suyu Dağıtım Ekibi"),
    /** @deprecated Retained only for Hibernate enum mapping safety — V8 migration converts DB rows to OTHER */
    @Deprecated
    LOGISTICS("Lojistik Ekibi"),
    EVACUATION("Tahliye Ekibi"),
    COMMUNICATION("İletişim Ekibi"),
    PSYCHOSOCIAL("Psikososyal Destek Ekibi"),
    HASAR_TESPIT_EKIBI("Hasar Tespit Ekibi"),
    OTHER("Diğer");

    private final String label;

    TeamName(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
