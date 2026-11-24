package com.studywithus.admin.application;

public interface AdminSystemDiagnosticsPort {
    SystemInfoResult getSystemInfo();
    SystemHealthResult getSystemHealth();
    SystemOverviewResult getSystemOverview();
}
