package com.afet.koordinasyon.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpInputMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Item 41 — her hatanın öngörülebilir, standart bir ErrorResponse'a çevrildiğini ve
 * client'a internal detay sızdırılmadığını doğrular.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void optimisticLockingFailure_mapsTo409WithTurkishMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/resource-stocks/transfer");
        var ex = new ObjectOptimisticLockingFailureException("ResourceStock", "id-123");

        var response = handler.handleOptimisticLocking(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Stok başka bir işlem tarafından güncellendi. Lütfen tekrar deneyin.");
        assertThat(response.getBody().getPath()).isEqualTo("/api/resource-stocks/transfer");
    }

    @Test
    void jpaEntityNotFound_mapsTo404_withoutLeakingInternalMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/events/123");
        var ex = new EntityNotFoundException(
                "com.afet.koordinasyon.domain.entity.Event with id 123 not found in table events");

        var response = handler.handleJpaEntityNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).doesNotContain("com.afet.koordinasyon.domain.entity");
        assertThat(response.getBody().getMessage()).isEqualTo("İstenen kayıt bulunamadı.");
    }

    @Test
    void maxUploadSizeExceeded_mapsTo413() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/users/me/documents");
        var ex = new MaxUploadSizeExceededException(10L * 1024 * 1024);

        var response = handler.handleMaxUploadSize(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().getStatus()).isEqualTo(413);
    }

    @Test
    void rateLimitExceeded_mapsTo429_withRetryAfterHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        var ex = new RateLimitExceededException("Çok fazla deneme yapıldı. Lütfen kısa süre sonra tekrar deneyin.", 60);

        var response = handler.handleRateLimit(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
    }

    @Test
    void dataIntegrityViolation_mapsTo409_withoutLeakingSqlDetail() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/resource-stocks");
        var ex = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uq_daa_active_assignment\"");

        var response = handler.handleDataIntegrityViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).doesNotContain("uq_daa_active_assignment");
    }

    @Test
    void malformedJsonBody_mapsTo400() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/events");
        var ex = new HttpMessageNotReadableException("JSON parse error", mock(HttpInputMessage.class));

        var response = handler.handleMalformedRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("İstek gövdesi veya parametreleri okunamadı.");
    }

    @Test
    void unhandledException_mapsTo500_withoutLeakingStackTraceOrClassName() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/events");
        var ex = new RuntimeException("db connection refused at jdbc:postgresql://internal-db:5432/afetdb");

        var response = handler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Beklenmeyen bir hata oluştu.");
        assertThat(response.getBody().getMessage()).doesNotContain("jdbc:postgresql");
    }

    @Test
    void errorResponse_includesRequestIdFromMdc() {
        MDC.put("requestId", "test-request-id-123");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/events/123");

        var response = handler.handleJpaEntityNotFound(new EntityNotFoundException("x"), request);

        assertThat(response.getBody().getRequestId()).isEqualTo("test-request-id-123");
    }
}
