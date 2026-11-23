package com.studywithus.media.domain;

import java.util.UUID;

public record MediaConfirmedEvent(UUID mediaId, UUID ownerId, String storageKey, String mimeType) {
}
