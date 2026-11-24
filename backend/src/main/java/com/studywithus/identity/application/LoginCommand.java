package com.studywithus.identity.application;

public record LoginCommand(
        String usernameOrEmail,
        String password,
        String deviceId,
        String totpCode
) {
    public LoginCommand(String usernameOrEmail, String password, String deviceId) {
        this(usernameOrEmail, password, deviceId, null);
    }
}
