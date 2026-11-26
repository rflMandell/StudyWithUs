package com.studywithus.checkin.application;

import java.util.UUID;

public record ToggleCheckinLikeCommand(
        UUID checkinId,
        UUID userId
) {
}
