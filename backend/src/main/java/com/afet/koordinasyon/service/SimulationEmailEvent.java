package com.afet.koordinasyon.service;

import java.util.UUID;

/**
 * Spring ApplicationEvent: simülasyon kaydı DB'ye commit edildikten sonra
 * async e-posta gönderimini tetiklemek için kullanılır.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) ile bu event yakalanır;
 * bu sayede e-posta gönderimi ana işlem transaction'ı dışında, ayrı bir
 * thread pool thread'i üzerinde çalışır.
 */
public record SimulationEmailEvent(UUID simulationId) {}
