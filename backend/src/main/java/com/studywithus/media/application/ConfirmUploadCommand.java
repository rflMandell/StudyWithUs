package com.studywithus.media.application;

import java.util.UUID;

public record ConfirmUploadCommand(
        UUID mediaId,
        UUID userId
) {
}
