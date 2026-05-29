package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.response.AuditLogResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.domain.entity.AuditLog;
import com.afet.koordinasyon.repository.AuditLogRepository;
import com.afet.koordinasyon.repository.spec.AuditLogSpecification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs", description = "Audit log management (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "List audit logs with filters and pagination")
    public ResponseEntity<PagedResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Boolean isSystemAction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        var spec = AuditLogSpecification.filter(
                actionType, actorId, actorRole, entityType, entityId,
                isSystemAction, fromDate, toDate, search);

        Page<AuditLogResponse> result = auditLogRepository
                .findAll(spec, PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(AuditLogResponse::from);

        return ResponseEntity.ok(PagedResponse.from(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single audit log detail")
    public ResponseEntity<AuditLogResponse> getAuditLog(@PathVariable UUID id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/export")
    @Operation(summary = "Export audit logs as CSV")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Boolean isSystemAction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(required = false) String search) {

        var spec = AuditLogSpecification.filter(
                actionType, actorId, actorRole, entityType, entityId,
                isSystemAction, fromDate, toDate, search);

        var logs = auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("id,actorId,actorName,actorRole,action,entityType,entityId,description,ipAddress,isSystemAction,createdAt");
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        for (AuditLog log : logs) {
            pw.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    nullSafe(log.getId()),
                    nullSafe(log.getActorId()),
                    csvEscape(log.getActorName()),
                    nullSafe(log.getActorRole()),
                    nullSafe(log.getAction()),
                    nullSafe(log.getEntityType()),
                    nullSafe(log.getEntityId()),
                    csvEscape(log.getDescription()),
                    nullSafe(log.getIpAddress()),
                    log.isSystemAction(),
                    log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "");
        }

        byte[] csv = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit_logs.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    private String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
