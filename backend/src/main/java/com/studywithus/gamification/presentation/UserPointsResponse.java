package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.UserPointsSummaryResult;

import java.util.UUID;

public record UserPointsResponse(
        UUID userId,
        long currentPoints,
        long lifetimePoints
) {
    public static UserPointsResponse from(UserPointsSummaryResult result) {
        return new UserPointsResponse(
                result.userId(),
                result.currentPoints(),
                result.lifetimePoints()
        );
    }
}
