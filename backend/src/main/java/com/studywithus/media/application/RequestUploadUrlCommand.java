package com.studywithus.media.application;

import com.studywithus.media.domain.MediaPurpose;

import java.util.UUID;

public record RequestUploadUrlCommand(
        UUID userId,
        MediaPurpose purpose,
        String filename,
        String contentType,
        long sizeBytes,
        UUID targetId
) {
}
