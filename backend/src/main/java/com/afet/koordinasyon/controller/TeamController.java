package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.dto.response.TeamMemberResponse;
import com.afet.koordinasyon.dto.response.TeamResponse;
import com.afet.koordinasyon.dto.response.TeamTypeResponse;
import com.afet.koordinasyon.service.TeamService;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team listing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @Operation(summary = "List all teams (authenticated users only)")
    public ResponseEntity<List<TeamResponse>> listTeams() {
        return ResponseEntity.ok(teamService.listAll());
    }

    @GetMapping("/types")
    @Operation(summary = "List all team types (static enum values — no DB enum mapping involved, public)")
    public ResponseEntity<List<TeamTypeResponse>> listTypes() {
        return ResponseEntity.ok(teamService.listTypes());
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN','DISTRICT_COORDINATOR','NEIGHBORHOOD_COORDINATOR')")
    @Operation(summary = "List active members of a team (contains personal data — admin/coordinator only)")
    public ResponseEntity<PagedResponse<TeamMemberResponse>> listMembers(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(teamService.listMembers(id, page, size));
    }

    @PostMapping("/backfill-codes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ekip kodu olmayan ekiplere otomatik kod atar (ADMIN)")
    public ResponseEntity<Map<String, Integer>> backfillCodes() {
        int count = teamService.backfillTeamCodes();
        return ResponseEntity.ok(Map.of("backfilledCount", count));
    }
}
