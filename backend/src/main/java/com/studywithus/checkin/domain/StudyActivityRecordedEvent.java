package com.studywithus.checkin.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudyActivityRecordedEvent(
        UUID checkinId,
        UUID userId,
        UUID communityId,
        String subject,
        List<String> tags,
        int durationMinutes,
        Instant occurredAt
) {
}
