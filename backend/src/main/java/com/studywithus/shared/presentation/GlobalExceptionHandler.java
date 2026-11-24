package com.studywithus.shared.presentation;

import com.studywithus.shared.application.SystemLogIngestionPort;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.infrastructure.logging.SensitiveDataMasker;
import com.studywithus.shared.infrastructure.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectProvider<SystemLogIngestionPort> systemLogIngestionPortProvider;

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String safeMessage = SensitiveDataMasker.mask(ex.getMessage());
        log.warn("Domain exception [traceId={}]: status={} code={} msg={}", traceId, ex.getStatus(), ex.getCode(), safeMessage);
        recordExceptionLog("WARN", ex.getStatus().value(), ex.getCode(), safeMessage, ex, request, false);

        ApiErrorResponse body = ApiErrorResponse.of(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        log.warn("Bad credentials [traceId={}]: {}", traceId, ex.getMessage());

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Invalid username/email or password",
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        log.warn("Authentication failed [traceId={}]: {}", traceId, ex.getMessage());

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                ex.getMessage(),
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        log.warn("Access denied [traceId={}]: {}", traceId, ex.getMessage());

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "You do not have permission to access this resource",
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error [traceId={}]: {}", traceId, detail);

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                detail,
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String safeMessage = SensitiveDataMasker.mask(ex.getMessage());
        log.error("Unhandled exception [traceId={}]: {}", traceId, safeMessage, ex);
        recordExceptionLog("ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", safeMessage, ex, request, true);

        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred",
                request.getRequestURI(),
                traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private void recordExceptionLog(
            String level,
            int statusCode,
            String errorCode,
            String message,
            Throwable ex,
            HttpServletRequest request,
            boolean includeStackTrace
    ) {
        String traceId = getTraceId();
        String requestId = MDC.get(TraceIdFilter.REQUEST_ID_MDC_KEY);
        String module = TraceIdFilter.resolveModule(request.getRequestURI());
        String userIdRaw = MDC.get(TraceIdFilter.USER_ID_MDC_KEY);
        UUID userId = null;
        if (userIdRaw != null && !userIdRaw.isBlank()) {
            try {
                userId = UUID.fromString(userIdRaw);
            } catch (IllegalArgumentException ignored) {
            }
        }

        String stackSummary = includeStackTrace
                ? Arrays.stream(ex.getStackTrace()).limit(8).map(StackTraceElement::toString).collect(Collectors.joining("\n"))
                : null;

        Map<String, Object> context = stackSummary != null
                ? Map.of("errorCode", errorCode, "exception", ex.getClass().getName(), "stackTrace", stackSummary)
                : Map.of("errorCode", errorCode, "exception", ex.getClass().getName());

        final UUID finalUserId = userId;
        systemLogIngestionPortProvider.ifAvailable(ingestion -> ingestion.recordAsync(
                Instant.now(),
                level,
                module,
                traceId,
                requestId,
                finalUserId,
                request.getMethod(),
                request.getRequestURI(),
                statusCode,
                null,
                errorCode + ": " + (message != null ? message : ""),
                context
        ));
    }

    private String getTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : "unknown";
    }
}
