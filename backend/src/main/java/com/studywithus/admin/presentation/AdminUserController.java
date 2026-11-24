package com.studywithus.admin.presentation;

import com.studywithus.admin.application.ListAdminUsersUseCase;
import com.studywithus.identity.domain.User;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final ListAdminUsersUseCase listAdminUsersUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<AdminUserSummaryResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<User> users = listAdminUsersUseCase.execute(page, size);
        return ResponseEntity.ok(PagedResponse.from(users, AdminUserSummaryResponse::from));
    }
}
