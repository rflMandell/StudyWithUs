package com.studywithus.admin.presentation;

import com.studywithus.admin.application.AdminAccountManagementUseCase;
import com.studywithus.admin.application.AdminUserFilter;
import com.studywithus.admin.application.AdminUserListItem;
import com.studywithus.admin.application.GetAdminUser360UseCase;
import com.studywithus.admin.application.ListAdminUsersUseCase;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final ListAdminUsersUseCase listAdminUsersUseCase;
    private final GetAdminUser360UseCase getAdminUser360UseCase;
    private final AdminAccountManagementUseCase adminAccountManagementUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<AdminUserSummaryResponse>> listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) GlobalRole globalRole,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminUserFilter filter = new AdminUserFilter(status, globalRole, plan, createdFrom, createdTo, search);
        PagedResult<AdminUserListItem> users = listAdminUsersUseCase.execute(filter, page, size);
        return ResponseEntity.ok(PagedResponse.from(users, AdminUserSummaryResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminUser360Response> getUser360(@PathVariable UUID id) {
        return ResponseEntity.ok(AdminUser360Response.from(getAdminUser360UseCase.execute(id)));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminUserSummaryResponse> suspendUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminAccountActionRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        User updated = adminAccountManagementUseCase.suspendUser(principal.getId(), id, request.reason());
        return ResponseEntity.ok(AdminUserSummaryResponse.from(updated));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminUserSummaryResponse> reactivateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminAccountActionRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        User updated = adminAccountManagementUseCase.reactivateUser(principal.getId(), id, request.reason());
        return ResponseEntity.ok(AdminUserSummaryResponse.from(updated));
    }

    @PostMapping("/{id}/revoke-sessions")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> revokeSessions(
            @PathVariable UUID id,
            @Valid @RequestBody AdminAccountActionRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        adminAccountManagementUseCase.revokeUserSessions(principal.getId(), id, request.reason());
        return ResponseEntity.ok(Map.of("userId", id, "sessionsRevoked", true));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminUserSummaryResponse> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRoleChangeRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        User updated = adminAccountManagementUseCase.changeGlobalRole(
                principal.getId(), id, request.globalRole(), request.reason()
        );
        return ResponseEntity.ok(AdminUserSummaryResponse.from(updated));
    }
}
