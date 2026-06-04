package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.CreateEventRequest;
import com.afet.koordinasyon.dto.request.UpdateEventRequest;
import com.afet.koordinasyon.dto.response.*;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.EventAssignmentService;
import com.afet.koordinasyon.service.EventService;
import com.afet.koordinasyon.service.TeamRecommendationService;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;
    private final TeamRecommendationService recommendationService;
    private final EventAssignmentService assignmentService;

    @GetMapping
    @Operation(summary = "List events with pagination and optional filters")
    public ResponseEntity<PagedResponse<EventSummaryResponse>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam(required = false) UUID teamId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.listEvents(page, size, status, districtId, neighborhoodId, teamId, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.getEventById(id, principal));
    }

    @PostMapping
    @Operation(summary = "Create a new event")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(request, principal));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event title/description/urgency/requiredPeople — ADMIN/DISTRICT_COORDINATOR/NEIGHBORHOOD_COORDINATOR only")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, principal));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark event as COMPLETED — ADMIN/DISTRICT_COORDINATOR/NEIGHBORHOOD_COORDINATOR only")
    public ResponseEntity<EventCloseResponse> completeEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.completeEvent(id, principal));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join an event as a volunteer")
    public ResponseEntity<EventJoinResponse> joinEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.joinEvent(id, principal));
    }

    @DeleteMapping("/{id}/leave")
    @Operation(summary = "Leave an event")
    public ResponseEntity<Void> leaveEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        eventService.leaveEvent(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close an event — ADMIN/DISTRICT_COORDINATOR/NEIGHBORHOOD_COORDINATOR only")
    public ResponseEntity<EventCloseResponse> closeEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.closeEvent(id, principal));
    }

    @GetMapping("/my")
    @Operation(summary = "Get events I have joined")
    public ResponseEntity<PagedResponse<UserEventResponse>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String volunteerStatus,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.getMyEvents(principal.getId(), page, size, volunteerStatus));
    }

    @GetMapping("/{id}/volunteers")
    @Operation(summary = "List volunteers for an event")
    public ResponseEntity<PagedResponse<EventVolunteerResponse>> getEventVolunteers(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(eventService.getEventVolunteers(id, page, size));
    }

    @GetMapping("/active-count")
    @Operation(summary = "Aktif (OPEN+IN_PROGRESS) ekip ihtiyacı sayısı — dashboard için")
    public ResponseEntity<Map<String, Long>> getActiveEventCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = eventService.countActiveEvents(principal);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/team-recommendations")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Etkinlik için AI ekip önerisi oluştur")
    public ResponseEntity<TeamRecommendationResponse> createEventRecommendation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.createRecommendationForEvent(id, principal));
    }

    @GetMapping("/{id}/team-recommendations/latest")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Etkinlik için en güncel AI ekip önerisini getir (otomatik öneri için). "
            + "Henüz öneri yoksa 204 döner — frontend kısa süre poll edebilir.")
    public ResponseEntity<TeamRecommendationResponse> getLatestEventRecommendation(@PathVariable UUID id) {
        return recommendationService.getLatestForEvent(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "Etkinliğe davet edilen / kabul eden kişileri listele")
    public ResponseEntity<List<EventAssignmentResponse>> getEventAssignments(
            @PathVariable UUID id) {
        return ResponseEntity.ok(assignmentService.getEventAssignments(id));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Birleşik katılımcı listesi — doğrudan katılanlar + AI davetliler")
    public ResponseEntity<List<EventParticipantResponse>> getEventParticipants(
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventParticipants(id));
    }

    @GetMapping("/{id}/team-recommendations/debug-candidates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "AI aday debug listesi — ADMIN only")
    public ResponseEntity<List<Map<String, Object>>> debugCandidates(@PathVariable UUID id) {
        return ResponseEntity.ok(recommendationService.debugCandidates(id));
    }
}
