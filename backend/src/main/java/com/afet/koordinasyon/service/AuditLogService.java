package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.AuditLog;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.repository.AuditLogRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

/**
 * Merkezi audit log servisi.
 * Tüm kritik işlemler buradan loglanır; try-catch ile sarmalanmış olduğu için
 * log hatası hiçbir zaman ana işlemi patlatmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    // ── Kullanıcı kaynaklı aksiyonlar ─────────────────────────────────────────

    public void logUserAction(
            UserPrincipal actor,
            AuditActionType actionType,
            String entityType,
            UUID entityId,
            String description,
            Map<String, Object> metadata) {

        persist(AuditLog.builder()
                .actorId(actor.getId())
                .actorName(actor.getFirstName() + " " + actor.getLastName())
                .actorRole(actor.getRole().name())
                .action(actionType.name())
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .metadata(toJson(metadata))
                .ipAddress(resolveClientIp())
                .userAgent(resolveUserAgent())
                .isSystemAction(false)
                .build());
    }

    /** Overload: sadece actorId + role biliniyorken (e.g. login). */
    public void logUserAction(
            UUID actorId,
            String actorName,
            String actorRole,
            AuditActionType actionType,
            String entityType,
            UUID entityId,
            String description,
            Map<String, Object> metadata) {

        persist(AuditLog.builder()
                .actorId(actorId)
                .actorName(actorName)
                .actorRole(actorRole)
                .action(actionType.name())
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .metadata(toJson(metadata))
                .ipAddress(resolveClientIp())
                .userAgent(resolveUserAgent())
                .isSystemAction(false)
                .build());
    }

    // ── Sistem kaynaklı aksiyonlar ─────────────────────────────────────────────

    public void logSystemAction(
            AuditActionType actionType,
            String entityType,
            UUID entityId,
            String description,
            Map<String, Object> metadata) {

        persist(AuditLog.builder()
                .actorId(null)
                .actorName("SYSTEM")
                .actorRole("SYSTEM")
                .action(actionType.name())
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .metadata(toJson(metadata))
                .ipAddress(null)
                .userAgent(null)
                .isSystemAction(true)
                .build());
    }

    // ── Kısayol metodlar ───────────────────────────────────────────────────────

    public void logRoleChange(
            UserPrincipal actor,
            UUID targetUserId,
            String targetUserName,
            String oldRole,
            String newRole) {

        logUserAction(actor, AuditActionType.ROLE_CHANGED, "USER", targetUserId,
                targetUserName + " kullanıcısının rolü " + oldRole + " → " + newRole + " olarak değiştirildi",
                Map.of("oldRole", oldRole, "newRole", newRole, "targetUser", targetUserName));
    }

    public void logCoordinatorAssigned(
            UserPrincipal actor,
            UUID assignedUserId,
            String assignedUserName,
            String scope,
            String scopeName) {

        logUserAction(actor, AuditActionType.COORDINATOR_ASSIGNED, "USER", assignedUserId,
                assignedUserName + " kullanıcısı " + scope + " koordinatörü atandı: " + scopeName,
                Map.of("assignedUser", assignedUserName, "scope", scope, "scopeName", scopeName));
    }

    public void logEarthquakeTrigger(UUID earthquakeId, String externalId, double magnitude, String location) {
        logSystemAction(AuditActionType.EARTHQUAKE_AUTO_TRIGGER, "EarthquakeEvent", earthquakeId,
                "AFAD depremi otomatik bildirim tetiklendi: M" + magnitude + " — " + location,
                Map.of("externalId", externalId, "magnitude", magnitude, "location", location));
    }

    // ── Dahili yardımcılar ─────────────────────────────────────────────────────

    private void persist(AuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Audit log kaydedilemedi [action={}]: {}", entry.getAction(), e.getMessage());
        }
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Audit metadata JSON dönüştürülemedi: {}", e.getMessage());
            return null;
        }
    }

    /** HTTP isteği bağlamından istemci IP'sini çözer. System action'larda null döner. */
    private String resolveClientIp() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveUserAgent() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                String ua = sra.getRequest().getHeader("User-Agent");
                return ua != null && ua.length() > 500 ? ua.substring(0, 500) : ua;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
