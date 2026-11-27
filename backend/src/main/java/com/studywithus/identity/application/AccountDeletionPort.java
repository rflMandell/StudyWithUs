package com.studywithus.identity.application;

import java.util.UUID;

public interface AccountDeletionPort {
    void deleteUserPermanently(UUID userId);
}
