package com.studywithus.identity.application;

public record LoginCommand(
        String usernameOrEmail,
        String password,
        String deviceId
) {
}
