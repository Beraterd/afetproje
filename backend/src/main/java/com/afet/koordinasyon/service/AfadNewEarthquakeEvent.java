package com.afet.koordinasyon.service;

import java.util.UUID;

/**
 * Yeni bir AFAD deprem kaydı DB'ye commit edildikten sonra
 * async bildirim gönderimini tetiklemek için kullanılır.
 * @TransactionalEventListener(phase = AFTER_COMMIT) ile yakalanır.
 */
public record AfadNewEarthquakeEvent(UUID earthquakeEventId) {}
