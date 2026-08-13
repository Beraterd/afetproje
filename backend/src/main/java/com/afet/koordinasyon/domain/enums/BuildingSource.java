package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BuildingSource {
    OPENSTREETMAP("OpenStreetMap"),
    MUNICIPALITY("Belediye"),
    OTHER("Diğer");

    private final String label;
}
