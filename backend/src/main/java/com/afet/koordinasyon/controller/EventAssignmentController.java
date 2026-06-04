package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.service.EventAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/event-assignments")
@RequiredArgsConstructor
@Tag(name = "EventAssignments", description = "Etkinlik davet kabul/red endpoints")
public class EventAssignmentController {

    private final EventAssignmentService assignmentService;

    @GetMapping("/accept")
    @Operation(summary = "Maildeki davet linkiyle görevi kabul et")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @RequestParam String token) {
        return ResponseEntity.ok(assignmentService.acceptInvitation(token));
    }

    @GetMapping("/decline")
    @Operation(summary = "Maildeki link ile daveti reddet")
    public ResponseEntity<Map<String, String>> declineInvitation(
            @RequestParam String token) {
        return ResponseEntity.ok(assignmentService.declineInvitation(token));
    }
}
