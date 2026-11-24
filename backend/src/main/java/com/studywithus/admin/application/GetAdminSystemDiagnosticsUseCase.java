package com.studywithus.admin.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAdminSystemDiagnosticsUseCase {

    private final AdminSystemDiagnosticsPort diagnosticsPort;

    public SystemInfoResult getInfo() {
        return diagnosticsPort.getSystemInfo();
    }

    public SystemHealthResult getHealth() {
        return diagnosticsPort.getSystemHealth();
    }

    public SystemOverviewResult getOverview() {
        return diagnosticsPort.getSystemOverview();
    }
}
