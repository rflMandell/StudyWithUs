package com.studywithus.admin.presentation;

import com.studywithus.admin.application.GetAdminSystemDiagnosticsUseCase;
import com.studywithus.admin.application.SystemHealthResult;
import com.studywithus.admin.application.SystemInfoResult;
import com.studywithus.admin.application.SystemOverviewResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final GetAdminSystemDiagnosticsUseCase diagnosticsUseCase;

    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SystemInfoResult> getSystemInfo() {
        return ResponseEntity.ok(diagnosticsUseCase.getInfo());
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SystemHealthResult> getSystemHealth() {
        return ResponseEntity.ok(diagnosticsUseCase.getHealth());
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SystemOverviewResult> getSystemOverview() {
        return ResponseEntity.ok(diagnosticsUseCase.getOverview());
    }
}
