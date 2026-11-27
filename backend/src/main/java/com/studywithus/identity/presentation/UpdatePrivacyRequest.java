package com.studywithus.identity.presentation;

import com.studywithus.identity.application.UpdateAccountPrivacyCommand;

import java.util.UUID;

public record UpdatePrivacyRequest(boolean isPrivate) {
    public UpdateAccountPrivacyCommand toCommand(UUID userId) {
        return new UpdateAccountPrivacyCommand(userId, isPrivate);
    }
}
