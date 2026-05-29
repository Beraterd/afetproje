package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.EarthquakeEvent;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.SystemNotification;
import com.afet.koordinasyon.domain.enums.NotificationType;
import com.afet.koordinasyon.dto.response.NotificationResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.UnreadCountResponse;
import com.afet.koordinasyon.exception.ResourceNotFoundException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.EarthquakeEventRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.SystemNotificationRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SystemNotificationRepository notifRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final EarthquakeEventRepository earthquakeEventRepository;

    // ── Spring Event Listener: Yeni Deprem ───────────────────────────────────

    /** Yeni AFAD depremi commit sonrası admin bildirimi oluşturur. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewEarthquake(AfadNewEarthquakeEvent event) {
        EarthquakeEvent eq = earthquakeEventRepository.findById(event.earthquakeEventId()).orElse(null);
        if (eq == null) return;
        String title = String.format("Yeni Deprem: M%.1f — %s",
                eq.getMagnitude() != null ? eq.getMagnitude() : 0.0,
                eq.getLocation() != null ? eq.getLocation() : "Konum bilinmiyor");
        String message = String.format("Büyüklük: M%.1f | Derinlik: %s km | Kaynak: AFAD",
                eq.getMagnitude() != null ? eq.getMagnitude() : 0.0,
                eq.getDepth() != null ? String.format("%.1f", eq.getDepth()) : "-");
        createForAdmins(NotificationType.NEW_EARTHQUAKE, title, message,
                "EarthquakeEvent", eq.getId().toString());
    }

    // ── Oluşturma metodları ───────────────────────────────────────────────────

    /** Adminlere yönelik sistem geneli bildirim (target_role = ADMIN). */
    @Transactional
    public void createForAdmins(NotificationType type, String title, String message,
                                String relatedEntityType, String relatedEntityId) {
        save(SystemNotification.builder()
                .targetRole("ADMIN")
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build());
    }

    /** Belirli bir ilçenin koordinatörüne bildirim (district_id). */
    @Transactional
    public void createForDistrict(UUID districtId, NotificationType type, String title, String message,
                                  String relatedEntityType, String relatedEntityId) {
        if (districtId == null) return;
        District district = districtRepository.findById(districtId).orElse(null);
        if (district == null) {
            log.warn("createForDistrict: district bulunamadı id={}", districtId);
            return;
        }
        save(SystemNotification.builder()
                .district(district)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build());
    }

    /** Belirli bir mahallenin koordinatörüne bildirim (neighborhood_id). */
    @Transactional
    public void createForNeighborhood(UUID neighborhoodId, NotificationType type, String title, String message,
                                      String relatedEntityType, String relatedEntityId) {
        if (neighborhoodId == null) return;
        Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId).orElse(null);
        if (neighborhood == null) {
            log.warn("createForNeighborhood: neighborhood bulunamadı id={}", neighborhoodId);
            return;
        }
        save(SystemNotification.builder()
                .neighborhood(neighborhood)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build());
    }

    // ── Sorgulama metodları ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getNotifications(UserPrincipal principal,
                                                                int page, int size,
                                                                String typeName,
                                                                Boolean isRead) {
        NotificationType type = parseType(typeName);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SystemNotification> result = notifRepository.findVisible(
                principal.getId(),
                principal.getRole().name(),
                principal.getDistrictId(),
                principal.getNeighborhoodId(),
                type,
                isRead,
                pageable);
        return PagedResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UserPrincipal principal) {
        long count = notifRepository.countUnread(
                principal.getId(),
                principal.getRole().name(),
                principal.getDistrictId(),
                principal.getNeighborhoodId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public void markAsRead(UUID notifId, UserPrincipal principal) {
        SystemNotification n = notifRepository.findById(notifId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notifId));
        if (n.isRead()) return;

        // Görünürlük kontrolü: bu kullanıcı bu bildirimi görebilmeli
        if (!isVisibleTo(n, principal)) {
            throw new AccessDeniedException("Bu bildirimi görme yetkiniz yok");
        }
        n.setRead(true);
        n.setReadAt(OffsetDateTime.now());
        notifRepository.save(n);
    }

    @Transactional
    public int markAllAsRead(UserPrincipal principal) {
        return notifRepository.markAllRead(
                principal.getId(),
                principal.getRole().name(),
                principal.getDistrictId(),
                principal.getNeighborhoodId(),
                OffsetDateTime.now());
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private void save(SystemNotification n) {
        try {
            notifRepository.save(n);
            log.debug("Bildirim oluşturuldu: type={}, targetRole={}, districtId={}, neighborhoodId={}",
                    n.getType(), n.getTargetRole(),
                    n.getDistrict() != null ? n.getDistrict().getId() : null,
                    n.getNeighborhood() != null ? n.getNeighborhood().getId() : null);
        } catch (Exception e) {
            log.error("Bildirim kaydedilemedi (type={}): {}", n.getType(), e.getMessage());
        }
    }

    private boolean isVisibleTo(SystemNotification n, UserPrincipal p) {
        String role = p.getRole().name();
        if (n.getRecipientUser() != null && n.getRecipientUser().getId().equals(p.getId())) return true;
        if (n.getTargetRole() != null && n.getTargetRole().equals(role)) return true;
        if (n.getDistrict() != null && p.getDistrictId() != null
                && n.getDistrict().getId().equals(p.getDistrictId())
                && "DISTRICT_COORDINATOR".equals(role)) return true;
        if (n.getNeighborhood() != null && p.getNeighborhoodId() != null
                && n.getNeighborhood().getId().equals(p.getNeighborhoodId())
                && "NEIGHBORHOOD_COORDINATOR".equals(role)) return true;
        return false;
    }

    private NotificationType parseType(String typeName) {
        if (typeName == null || typeName.isBlank()) return null;
        try { return NotificationType.valueOf(typeName.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private NotificationResponse toResponse(SystemNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
