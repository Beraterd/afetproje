package com.afet.koordinasyon.service;

/**
 * Gerçek deprem veya simülasyon kaydı DB'ye commit edildikten sonra,
 * ortak bildirim akışını (mail + WhatsApp) tetiklemek için publish edilir.
 * <p>
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} ile yakalanır;
 * hem gerçek deprem hem simülasyon aynı {@link EarthquakeAlertNotificationService}
 * üzerinden geçer.
 */
public record EarthquakeAlertCreatedEvent(EarthquakeAlert alert) {
}
