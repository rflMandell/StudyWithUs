package com.studywithus.moderation.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.moderation.application.ListAuditLogsQuery;
import com.studywithus.moderation.application.ListAuditLogsUseCase;
import com.studywithus.moderation.application.ModerationAuthorizationService;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.presentation.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final ListAuditLogsUseCase listAuditLogsUseCase;
    private final ModerationAuthorizationService moderationAuthorizationService;

    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogResponse>> listAuditLogs(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @RequestParam(required = false) UUID communityId,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (principal == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Autenticação obrigatória");
        }
        if (!moderationAuthorizationService.isPlatformAdmin()) {
            if (communityId != null) {
                moderationAuthorizationService.assertCommunityModerator(communityId, principal.getId());
            } else if (!moderationAuthorizationService.isModeratorOfAnyCommunity(principal.getId())) {
                throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Apenas moderadores podem visualizar logs de auditoria");
            }
        }

        var query = new ListAuditLogsQuery(targetType, page, size);
        var pagedResult = listAuditLogsUseCase.execute(query);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, AuditLogResponse::from));
    }
}
