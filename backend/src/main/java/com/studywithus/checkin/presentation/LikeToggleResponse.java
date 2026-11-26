package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.LikeToggleResult;

import java.util.UUID;

public record LikeToggleResponse(
        UUID targetId,
        boolean liked,
        long likesCount
) {
    public static LikeToggleResponse from(LikeToggleResult result) {
        return new LikeToggleResponse(result.targetId(), result.liked(), result.likesCount());
    }
}
