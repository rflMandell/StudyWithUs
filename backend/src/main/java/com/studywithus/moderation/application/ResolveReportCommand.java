package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.ModerationAction;

import java.util.UUID;

public record ResolveReportCommand(
        UUID reportId,
        UUID reviewerId,
        ModerationAction action,
        String notes
) {
    public ResolveReportCommand {
        if (action == null) {
            action = ModerationAction.REMOVE_CONTENT;
        }
    }
}
