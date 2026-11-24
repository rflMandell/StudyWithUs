package com.studywithus.admin.infrastructure;

import com.studywithus.admin.application.AdminAuditContextHolder;
import com.studywithus.admin.application.AdminAuditService;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

/**
 * Ensures every mutative request under /api/v1/admin/** that succeeded (2xx)
 * leaves an immutable audit trail even if the endpoint did not invoke AdminAuditService explicitly.
 */
@Component
@RequiredArgsConstructor
public class AdminMutationAuditInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATIVE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AdminAuditService adminAuditService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        try {
            if (ex == null
                    && MUTATIVE_METHODS.contains(request.getMethod().toUpperCase())
                    && response.getStatus() >= 200
                    && response.getStatus() < 300
                    && !AdminAuditContextHolder.wasRecorded()) {
                UUID actorId = extractActorId();
                if (actorId != null) {
                    String action = (request.getMethod() + " " + request.getRequestURI()).toUpperCase();
                    if (action.length() > 100) {
                        action = action.substring(0, 100);
                    }
                    adminAuditService.record(
                            actorId,
                            action,
                            "ADMIN_ENDPOINT",
                            null,
                            null,
                            null,
                            null
                    );
                }
            }
        } finally {
            AdminAuditContextHolder.clear();
        }
    }

    private UUID extractActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
