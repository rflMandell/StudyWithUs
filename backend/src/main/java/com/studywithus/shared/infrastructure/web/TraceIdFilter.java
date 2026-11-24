package com.studywithus.shared.infrastructure.web;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.application.SystemLogIngestionPort;
import com.studywithus.shared.infrastructure.logging.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String MODULE_MDC_KEY = "module";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String HTTP_METHOD_MDC_KEY = "httpMethod";
    public static final String PATH_MDC_KEY = "path";
    public static final String STATUS_CODE_MDC_KEY = "statusCode";
    public static final String DURATION_MS_MDC_KEY = "durationMs";

    private final ObjectProvider<SystemLogIngestionPort> systemLogIngestionPortProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startMs = System.currentTimeMillis();
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        String module = resolveModule(path);

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(MODULE_MDC_KEY, module);
        MDC.put(HTTP_METHOD_MDC_KEY, method);
        MDC.put(PATH_MDC_KEY, SensitiveDataMasker.mask(path));
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            int statusCode = response.getStatus();
            UUID userId = extractCurrentUserId();
            String level = statusCode >= 500 ? "ERROR" : (statusCode >= 400 ? "WARN" : "INFO");
            String message = String.format("%s %s -> %d (%dms)", method, SensitiveDataMasker.mask(path), statusCode, durationMs);

            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId.toString());
            }
            MDC.put(STATUS_CODE_MDC_KEY, String.valueOf(statusCode));
            MDC.put(DURATION_MS_MDC_KEY, String.valueOf(durationMs));

            if ("ERROR".equals(level)) {
                log.error(message);
            } else if ("WARN".equals(level)) {
                log.warn(message);
            } else {
                log.info(message);
            }

            if (path.startsWith("/api/")) {
                final String finalTraceId = traceId;
                final String finalRequestId = requestId;
                systemLogIngestionPortProvider.ifAvailable(ingestion -> ingestion.recordAsync(
                        Instant.now(),
                        level,
                        module,
                        finalTraceId,
                        finalRequestId,
                        userId,
                        method,
                        path,
                        statusCode,
                        durationMs,
                        message,
                        Map.of("query", SensitiveDataMasker.mask(request.getQueryString() != null ? request.getQueryString() : ""))
                ));
            }

            MDC.remove(TRACE_ID_MDC_KEY);
            MDC.remove(REQUEST_ID_MDC_KEY);
            MDC.remove(MODULE_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
            MDC.remove(HTTP_METHOD_MDC_KEY);
            MDC.remove(PATH_MDC_KEY);
            MDC.remove(STATUS_CODE_MDC_KEY);
            MDC.remove(DURATION_MS_MDC_KEY);
        }
    }

    private UUID extractCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    public static String resolveModule(String path) {
        if (path == null || !path.startsWith("/api/v1/")) {
            return "shared";
        }
        String remainder = path.substring("/api/v1/".length());
        int slash = remainder.indexOf('/');
        String segment = slash > 0 ? remainder.substring(0, slash) : remainder;
        return switch (segment) {
            case "auth", "users" -> "identity";
            case "communities" -> "community";
            case "checkins", "feed" -> "checkin";
            case "comments" -> "social";
            case "challenges", "badges" -> "gamification";
            case "leagues" -> "league";
            case "analytics" -> "analytics";
            case "notifications", "devices" -> "notification";
            case "reports", "audit-logs" -> "moderation";
            case "subscription", "webhooks" -> "subscription";
            case "uploads", "media" -> "media";
            case "admin" -> "admin";
            default -> "shared";
        };
    }
}
