package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.DamageAssessmentTaskResponse;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.MyTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/my-tasks")
@RequiredArgsConstructor
@Tag(name = "My Tasks", description = "Volunteer's assigned tasks (damage assessments)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class MyTaskController {

    private final MyTaskService myTaskService;

    @GetMapping("/damage-assessments")
    @Operation(summary = "List all damage assessment tasks assigned to the current user")
    public ResponseEntity<List<DamageAssessmentTaskResponse>> getMyDamageTasks(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(myTaskService.getMyTasks(principal));
    }

    @GetMapping("/damage-assessments/{assignmentId}")
    @Operation(summary = "Get detail of a specific damage assessment task")
    public ResponseEntity<DamageAssessmentTaskResponse> getTaskDetail(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(myTaskService.getTaskDetail(assignmentId, principal));
    }

    @PostMapping(value = "/damage-assessments/{assignmentId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload field photos for an assigned damage assessment task")
    public ResponseEntity<Void> uploadPhotos(
            @PathVariable UUID assignmentId,
            @RequestPart("photos") List<MultipartFile> photos,
            @AuthenticationPrincipal UserPrincipal principal) {
        myTaskService.uploadFieldPhotos(assignmentId, photos, principal);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/damage-assessments/{assignmentId}/field-verified")
    @Operation(summary = "Mark a damage assessment task as field-verified")
    public ResponseEntity<DamageAssessmentTaskResponse> markFieldVerified(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(myTaskService.markFieldVerified(assignmentId, principal));
    }

    @PostMapping("/damage-assessments/{assignmentId}/complete")
    @Operation(summary = "Complete a damage assessment task")
    public ResponseEntity<DamageAssessmentTaskResponse> completeTask(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(myTaskService.completeTask(assignmentId, principal));
    }
}
