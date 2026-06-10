package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.AssignRoleRequest;
import com.afet.koordinasyon.dto.request.UpdateLocationPermissionRequest;
import com.afet.koordinasyon.dto.request.UpdateNotificationPreferencesRequest;
import com.afet.koordinasyon.dto.request.UpdateProfileRequest;
import com.afet.koordinasyon.dto.response.AssemblyAreaResponse;
import com.afet.koordinasyon.dto.response.NotificationPreferencesResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.UserResponse;
import com.afet.koordinasyon.dto.response.UserSearchResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.AdminMaintenanceService;
import com.afet.koordinasyon.service.AssemblyAreaService;
import com.afet.koordinasyon.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AdminMaintenanceService adminMaintenanceService;
    private final AssemblyAreaService assemblyAreaService;

    @PutMapping("/api/users/me/location")
    @Operation(summary = "Update own location permission and last known coordinates")
    public ResponseEntity<UserResponse> updateLocationPermission(
            @RequestBody UpdateLocationPermissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateLocationPermission(principal.getId(), request));
    }

    @PutMapping("/api/users/me")
    @Operation(summary = "Update own profile")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request));
    }

    @GetMapping("/api/users/me/notification-preferences")
    @Operation(summary = "Get own email notification preferences")
    public ResponseEntity<NotificationPreferencesResponse> getNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getNotificationPreferences(principal.getId()));
    }

    @PutMapping("/api/users/me/notification-preferences")
    @Operation(summary = "Update own email notification preferences")
    public ResponseEntity<NotificationPreferencesResponse> updateNotificationPreferences(
            @RequestBody UpdateNotificationPreferencesRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateNotificationPreferences(principal.getId(), request));
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (Admin only)")
    public ResponseEntity<PagedResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.listUsers(page, size, role));
    }

    @PostMapping("/api/admin/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user (Admin only)")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID userId,
            @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.assignRole(userId, request.getRole(), principal));
    }

    @PutMapping("/api/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user profile (Admin only)")
    public ResponseEntity<UserResponse> adminUpdateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @DeleteMapping("/api/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hard-delete a user (Admin only)",
               description = "Kullanıcıyı ve bağlı tüm kayıtlarını kalıcı olarak siler. " +
                             "Protected admin kullanıcılar silinemez (422). " +
                             "Kendi hesabınızı silemezsiniz (422).")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        adminMaintenanceService.deleteUserHard(userId, principal.getId(), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/assembly-areas")
    @Operation(summary = "Get assembly areas in the current user's neighborhood (emergency page)")
    public ResponseEntity<List<AssemblyAreaResponse>> getMyAssemblyAreas(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(assemblyAreaService.getMyNeighborhoodAreas(principal.getId()));
    }

    @GetMapping("/api/users/search")
    @Operation(summary = "Search users by name or email (for emergency contact lookup)")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }
}
