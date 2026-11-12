package com.studywithus.notification.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.notification.application.RegisterDeviceTokenUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        registerDeviceTokenUseCase.execute(request.toCommand(principal.getId()));
    }
}
