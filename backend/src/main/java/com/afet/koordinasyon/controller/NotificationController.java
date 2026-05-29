package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.NotificationResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.UnreadCountResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Bildirim Merkezi")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Kullanıcının görebileceği bildirimleri sayfalı listele")
    public ResponseEntity<PagedResponse<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0")  int     page,
            @RequestParam(defaultValue = "20") int     size,
            @RequestParam(required = false)    String  type,
            @RequestParam(required = false)    Boolean isRead,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getNotifications(principal, page, size, type, isRead));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Okunmamış bildirim sayısını döner")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Tek bildirimi okundu olarak işaretle")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Kullanıcının tüm bildirimlerini okundu işaretle")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal);
        return ResponseEntity.noContent().build();
    }
}
