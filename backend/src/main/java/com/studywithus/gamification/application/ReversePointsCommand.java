package com.studywithus.gamification.application;

import java.util.UUID;

public record ReversePointsCommand(
        UUID originalTransactionId
) {
}
