package com.studywithus.admin.application;

import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAdminUsersUseCase {

    private final AdminUserQueryPort adminUserQueryPort;

    @Transactional(readOnly = true)
    public PagedResult<AdminUserListItem> execute(AdminUserFilter filter, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return adminUserQueryPort.findUsers(filter != null ? filter : AdminUserFilter.empty(), safePage, safeSize);
    }
}
