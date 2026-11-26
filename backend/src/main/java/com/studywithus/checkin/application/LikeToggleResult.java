package com.studywithus.checkin.application;

import java.util.UUID;

public record LikeToggleResult(
        UUID targetId,
        boolean liked,
        long likesCount
) {
}
