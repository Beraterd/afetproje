package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

/**
 * Public kısa link yönlendirme endpoint'i.
 * GET /s/{code} → targetUrl'e 302 redirect.
 * <p>
 * GÜVENLİK: Yalnızca yönlendirme yapar; hiçbir kişisel veri döndürmez.
 * Bu endpoint SecurityConfig içinde permitAll olarak işaretlenmiştir.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Short Links", description = "Public kısa link yönlendirme")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @GetMapping("/s/{code}")
    @Operation(summary = "Kısa linki çözüp hedef URL'e yönlendirir")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        Optional<String> target = shortLinkService.resolveTargetUrl(code);
        return target
                .map(url -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
