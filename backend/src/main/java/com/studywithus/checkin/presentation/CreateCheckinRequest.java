package com.studywithus.checkin.presentation;

import com.studywithus.checkin.application.CreateCheckinCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateCheckinRequest(
        UUID communityId,

        @Size(max = 1000, message = "Caption must not exceed 1000 characters")
        String caption,

        List<String> mediaKeys,

        @Size(max = 100, message = "Subject must not exceed 100 characters")
        String subject,

        List<String> tags,

        @Min(value = 0, message = "Duration must be non-negative")
        @Max(value = 1440, message = "Duration must not exceed 1440 minutes")
        Integer durationMinutes,

        Boolean countsAsCheckin
) {
    public CreateCheckinRequest(UUID communityId, String caption, List<String> mediaKeys) {
        this(communityId, caption, mediaKeys, null, null, null, null);
    }

    public CreateCheckinRequest(
            UUID communityId,
            String caption,
            List<String> mediaKeys,
            String subject,
            List<String> tags,
            Integer durationMinutes
    ) {
        this(communityId, caption, mediaKeys, subject, tags, durationMinutes, null);
    }

    public CreateCheckinCommand toCommand(UUID userId, String idempotencyKey) {
        return new CreateCheckinCommand(
                userId,
                communityId,
                caption,
                mediaKeys,
                idempotencyKey,
                subject,
                tags,
                durationMinutes,
                countsAsCheckin
        );
    }
}
