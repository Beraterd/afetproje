package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.dto.request.BulkReviewRequest;
import com.afet.koordinasyon.dto.request.ReviewAssemblyAreaRequest;
import com.afet.koordinasyon.dto.response.AssemblyAreaResponse;
import com.afet.koordinasyon.dto.response.AssemblyAreaStatsResponse;
import com.afet.koordinasyon.dto.response.AssemblyCoverageResponse;
import com.afet.koordinasyon.dto.response.PagedResponse;
import com.afet.koordinasyon.service.AssemblyAreaExcelImportService;
import com.afet.koordinasyon.service.AssemblyAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/assembly-areas")
@RequiredArgsConstructor
@Tag(name = "Admin Assembly Areas", description = "Toplanma alanı doğrulama ve yönetim (Admin)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssemblyAreaController {

    private final AssemblyAreaService assemblyAreaService;
    private final AssemblyAreaExcelImportService excelImportService;

    @GetMapping
    @Operation(summary = "Filtreli toplanma alanı listesi (admin)")
    public ResponseEntity<PagedResponse<AssemblyAreaResponse>> list(
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) UUID neighborhoodId,
            @RequestParam(required = false) String sourceName,
            @RequestParam(required = false) Boolean needsReview,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(assemblyAreaService.listWithFilters(
                districtId, neighborhoodId, sourceName, needsReview, active, search, page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "Doğrulama bekleyen kayıt sayısı ve toplam")
    public ResponseEntity<AssemblyAreaStatsResponse> getStats() {
        return ResponseEntity.ok(assemblyAreaService.getStats());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Toplanma alanı detayı")
    public ResponseEntity<AssemblyAreaResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(assemblyAreaService.getById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Toplanma alanı güncelle (koordinat, doğrulama durumu, aktiflik)")
    public ResponseEntity<AssemblyAreaResponse> update(
            @PathVariable UUID id,
            @RequestBody ReviewAssemblyAreaRequest request) {
        return ResponseEntity.ok(assemblyAreaService.update(id, request));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Toplu güncelleme: seçili kayıtları doğrulandı / pasif yap")
    public ResponseEntity<Map<String, Integer>> bulkUpdate(@RequestBody BulkReviewRequest request) {
        int updated = assemblyAreaService.bulkUpdate(request);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Excel dosyasından toplanma alanlarını içe aktar",
        description = """
            Beklenen Excel kolon sırası (başlık satırı otomatik atlanır):
              0: İlçe | 1: Mahalle | 2: Toplanma Alanı adı | 3: Alan_m2 (isteğe bağlı) | 4: Google Maps URL

            Davranış:
            - Aynı mahallede aynı isimli alan zaten varsa atlanır (idempotent).
            - İlçe/mahalle adı DB'dekiyle eşleşmezse 'unmatchedNeighborhoods' listesine eklenir.
            - Google Maps URL'i varsa lat/lng otomatik ayrıştırılır.
            - URL yoksa kayıt 'needs_review=TRUE' olarak işaretlenir (mail filtresini geçmez).
            """)
    public ResponseEntity<Map<String, Object>> importFromExcel(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Dosya boş. Lütfen geçerli bir .xlsx dosyası yükleyin."));
        }
        try (var inputStream = file.getInputStream()) {
            AssemblyAreaExcelImportService.ImportResult result = excelImportService.importFromExcel(inputStream);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("imported",               result.imported());
            body.put("skippedDuplicate",       result.skippedDuplicate());
            body.put("skippedNoNeighborhood",  result.skippedNoNeighborhood());
            body.put("unmatchedNeighborhoods", result.unmatchedNeighborhoods());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Import başarısız"));
        }
    }

    @GetMapping("/coverage")
    @Operation(
        summary = "İlçe/mahalle bazında toplanma alanı kapsama raporu",
        description = "Hangi ilçe ve mahallelerde doğrulanmış toplanma alanı var/yok gösterir. "
                    + "Import sonrası coverage analizi için kullanılır.")
    public ResponseEntity<AssemblyCoverageResponse> getCoverage() {
        return ResponseEntity.ok(assemblyAreaService.getCoverage());
    }
}
