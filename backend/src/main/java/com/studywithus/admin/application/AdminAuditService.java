package com.studywithus.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.shared.infrastructure.logging.SensitiveDataMasker;
import com.studywithus.shared.infrastructure.web.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional service for recording immutable, append-only administrative audit logs.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuditLog record(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            String reason,
            Object beforeState,
            Object afterState
    ) {
        String actorRole = resolveActorRole();
        HttpServletRequest request = currentRequest();
        String ipAddress = request != null ? extractClientIp(request) : null;
        String userAgent = request != null ? truncate(request.getHeader("User-Agent"), 500) : null;
        String requestId = MDC.get(TraceIdFilter.REQUEST_ID_MDC_KEY);

        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(SensitiveDataMasker.mask(reason))
                .beforeState(serializeSafe(beforeState))
                .afterState(serializeSafe(afterState))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestId(requestId)
                .createdAt(Instant.now())
                .build();

        AuditLog saved = auditLogRepository.save(entry);
        AdminAuditContextHolder.markRecorded();
        return saved;
    }

    private String resolveActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_") && !"ROLE_USER".equals(a))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("USER");
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String serializeSafe(Object state) {
        if (state == null) {
            return null;
        }
        try {
            String raw = state instanceof String s ? s : objectMapper.writeValueAsString(state);
            return SensitiveDataMasker.mask(raw);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
