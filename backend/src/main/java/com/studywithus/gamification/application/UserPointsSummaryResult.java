package com.studywithus.gamification.application;

import java.util.UUID;

public record UserPointsSummaryResult(
        UUID userId,
        long currentPoints,
        long lifetimePoints
) {
}
