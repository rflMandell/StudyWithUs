package com.studywithus.admin.application;

public record TwoFactorSetupResult(
        String secret,
        String otpAuthUri
) {
}
