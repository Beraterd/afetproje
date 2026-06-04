package com.afet.koordinasyon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * §3 — Olay oluşturulduğunda otomatik ekip önerisini arka planda tetikler.
 *
 * <p>Ayrı bir bean olmasının nedeni: {@code @Async} + {@code @Transactional} aynı sınıf
 * içinde self-invocation ile çalışmaz. Bu bean async sınırını sağlar ve proxy üzerinden
 * {@link TeamRecommendationService#autoGenerateForEvent(UUID, UUID)} transactional metodunu çağırır.
 * Öneri motorundaki herhangi bir hata ana olay oluşturma akışını ASLA bozmaz.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeamRecommendationAutoTrigger {

    private final TeamRecommendationService teamRecommendationService;

    @Async
    public void onEventCreated(UUID eventId, UUID requestedByUserId) {
        try {
            teamRecommendationService.autoGenerateForEvent(eventId, requestedByUserId);
        } catch (Exception e) {
            log.warn("Otomatik ekip önerisi üretilemedi (event={}): {}", eventId, e.getMessage());
        }
    }
}
