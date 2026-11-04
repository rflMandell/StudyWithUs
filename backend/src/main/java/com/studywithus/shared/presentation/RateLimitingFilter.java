package com.studywithus.shared.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.shared.infrastructure.RateLimitingService;
import com.studywithus.shared.infrastructure.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Target sensitive endpoints
        int maxRequests = 0;
        int windowSeconds = 60;

        if ("POST".equalsIgnoreCase(method) && path.endsWith("/api/v1/auth/login")) {
            maxRequests = 20; // 20 login attempts per minute per IP
        } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/api/v1/auth/register")) {
            maxRequests = 15; // 15 registrations per minute per IP
        }

        if (maxRequests > 0) {
            String clientIp = getClientIp(request);
            String rateLimitKey = method + ":" + path + ":" + clientIp;

            boolean allowed = rateLimitingService.tryAcquire(rateLimitKey, maxRequests, windowSeconds);
            if (!allowed) {
                log.warn("Rate limit exceeded for IP {} on {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(windowSeconds));
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
                ApiErrorResponse error = ApiErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "RATE_LIMIT_EXCEEDED",
                        "Too many requests. Please try again in " + windowSeconds + " seconds.",
                        path,
                        traceId != null ? traceId : "unknown"
                );

                objectMapper.writeValue(response.getOutputStream(), error);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
