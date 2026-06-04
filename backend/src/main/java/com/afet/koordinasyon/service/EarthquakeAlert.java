package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.entity.EarthquakeSimulation;
import com.afet.koordinasyon.domain.enums.EarthquakeSourceType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Gerçek deprem ve simülasyon için ortak bildirim veri modeli.
 * Hem AFAD deprem event'inden hem de deprem simülasyonundan üretilebilir ve
 * tek bir bildirim akışı (mail + WhatsApp) tarafından tüketilir.
 *
 * @param alertId    REAL için EarthquakeEvent id'si, SIMULATION için EarthquakeSimulation id'si
 * @param sourceType REAL veya SIMULATION
 */
public record EarthquakeAlert(
        UUID alertId,
        EarthquakeSourceType sourceType,
        Double magnitude,
        String location,
        String province,
        String district,
        OffsetDateTime occurredAt,
        Double latitude,
        Double longitude,
        Double depth
) {

    public boolean isSimulation() {
        return sourceType == EarthquakeSourceType.SIMULATION;
    }

    /** Gerçek AFAD deprem kaydından alert üretir. */
    public static EarthquakeAlert fromEvent(EarthquakeEvent eq) {
        return new EarthquakeAlert(
                eq.getId(),
                EarthquakeSourceType.REAL,
                eq.getMagnitude(),
                eq.getLocation(),
                eq.getProvince(),
                eq.getDistrict(),
                eq.getEventTime(),
                eq.getLatitude(),
                eq.getLongitude(),
                eq.getDepth()
        );
    }

    /**
     * Deprem simülasyonundan alert üretir. Simülasyonun ilçe adı konum olarak kullanılır;
     * gerçek depreme özgü koordinat/derinlik alanları null'dur.
     */
    public static EarthquakeAlert fromSimulation(EarthquakeSimulation sim) {
        String districtName = sim.getDistrict() != null ? sim.getDistrict().getName() : null;
        return new EarthquakeAlert(
                sim.getId(),
                EarthquakeSourceType.SIMULATION,
                sim.getMagnitude() != null ? sim.getMagnitude().doubleValue() : null,
                districtName,
                null,
                districtName,
                sim.getTriggeredAt(),
                null,
                null,
                null
        );
    }
}
