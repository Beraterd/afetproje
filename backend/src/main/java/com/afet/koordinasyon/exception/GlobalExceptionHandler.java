package com.afet.koordinasyon.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private String currentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }

    private ErrorResponse.ErrorResponseBuilder baseBuilder(int status, String error, String message,
            HttpServletRequest request) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .path(request.getRequestURI())
                .requestId(currentRequestId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        BindingResult result = ex.getBindingResult();
        List<ErrorResponse.FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse error = baseBuilder(400, "VALIDATION_ERROR", "Gönderilen bilgiler geçersiz.", request)
                .details(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(404, "NOT_FOUND", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJpaEntityNotFound(EntityNotFoundException ex,
            HttpServletRequest request) {
        logger.warn("Entity not found at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = baseBuilder(404, "NOT_FOUND", "İstenen kayıt bulunamadı.", request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(409, "CONFLICT", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        logger.warn("Optimistic lock conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = baseBuilder(409, "STALE_VERSION",
                "Stok başka bir işlem tarafından güncellendi. Lütfen tekrar deneyin.", request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        logger.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        ErrorResponse error = baseBuilder(409, "CONFLICT",
                "İşlem mevcut bir kayıtla çakışıyor veya bütünlük kuralını ihlal ediyor.", request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(ex.getStatus().value(), ex.getErrorCode(), ex.getMessage(), request).build();
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(429, "RATE_LIMITED", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(401, "UNAUTHORIZED", "E-posta veya şifre hatalı.", request).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(403, "FORBIDDEN", "Bu işlem için yetkiniz yok.", request).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        ErrorResponse error = baseBuilder(415, "UNSUPPORTED_MEDIA_TYPE",
                "Desteklenmeyen içerik türü. Beklenen: multipart/form-data", request).build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        ErrorResponse error = baseBuilder(413, "PAYLOAD_TOO_LARGE",
                "Yüklenen dosya izin verilen boyut sınırını aşıyor.", request).build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        logger.warn("Malformed request at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = baseBuilder(400, "MALFORMED_REQUEST",
                "İstek gövdesi veya parametreleri okunamadı.", request).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        logger.error("Unhandled exception [requestId={}]", currentRequestId(), ex);
        ErrorResponse error = baseBuilder(500, "INTERNAL_ERROR", "Beklenmeyen bir hata oluştu.", request).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
