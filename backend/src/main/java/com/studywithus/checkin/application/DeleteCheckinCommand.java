package com.studywithus.checkin.application;

import java.util.UUID;

public record DeleteCheckinCommand(
        UUID checkinId,
        UUID actorUserId
) {
}
