package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Download", description = "Public file download endpoint using secure token")
public class FileDownloadController {

    private final DocumentService documentService;

    @GetMapping("/download/{downloadToken}")
    @Operation(summary = "Download a file using a secure download token (no auth required)")
    public void download(@PathVariable UUID downloadToken, HttpServletResponse response) {
        documentService.serveFile(downloadToken, response);
    }
}
