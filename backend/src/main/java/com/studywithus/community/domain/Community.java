package com.studywithus.community.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Community {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
    private CommunityVisibility visibility;
    private UUID ownerId;
    private String rankingType;
    private String timezone;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public boolean isPublic() {
        return visibility == CommunityVisibility.PUBLIC;
    }

    public boolean isPrivate() {
        return visibility == CommunityVisibility.PRIVATE;
    }

    public boolean isActive() {
        return deletedAt == null;
    }
}
