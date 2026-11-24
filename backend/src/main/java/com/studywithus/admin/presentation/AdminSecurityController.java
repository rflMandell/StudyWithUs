package com.studywithus.admin.presentation;

import com.studywithus.admin.application.AdminSessionResult;
import com.studywithus.admin.application.AdminTwoFactorUseCase;
import com.studywithus.admin.application.TwoFactorSetupResult;
import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/security")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final AdminTwoFactorUseCase adminTwoFactorUseCase;

    @GetMapping("/session")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AdminSessionResult> getSession(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(adminTwoFactorUseCase.getSessionInfo(principal.getId()));
    }

    @PostMapping("/2fa/setup")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TwoFactorSetupResult> setupTwoFactor(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(adminTwoFactorUseCase.setupTwoFactor(principal.getId()));
    }

    @PostMapping("/2fa/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyTwoFactor(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody TwoFactorCodeRequest request
    ) {
        adminTwoFactorUseCase.verifyAndEnableTwoFactor(principal.getId(), request.code());
        return ResponseEntity.ok(Map.of("totpEnabled", true));
    }

    @DeleteMapping("/2fa")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableTwoFactor(
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody TwoFactorCodeRequest request
    ) {
        adminTwoFactorUseCase.disableTwoFactor(principal.getId(), request.code());
    }
}
