package com.studywithus.admin.application;

import com.studywithus.shared.domain.PagedResult;

import java.util.List;
import java.util.UUID;

public interface AdminUserQueryPort {
    PagedResult<AdminUserListItem> findUsers(AdminUserFilter filter, int page, int size);
    List<AdminUser360Result.CommunitySummary> findUserCommunities(UUID userId);
    List<AdminUser360Result.ReportSummary> findUserReports(UUID userId);
}
