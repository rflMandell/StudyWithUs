package com.studywithus.community.presentation;

import com.studywithus.community.application.CreateCommunityCommand;
import com.studywithus.community.domain.CommunityVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommunityRequest(
        @NotBlank(message = "Community name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        String imageUrl,

        @NotNull(message = "Visibility is required")
        CommunityVisibility visibility,

        String timezone
) {
    public CreateCommunityCommand toCommand(UUID ownerId) {
        return new CreateCommunityCommand(
                name,
                description,
                imageUrl,
                visibility,
                timezone,
                ownerId
        );
    }
}
