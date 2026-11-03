package com.studywithus.gamification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {
    private UUID userId;
    private UUID badgeId;
    private String name;
    private String description;
    private String imageUrl;
    private Instant awardedAt;
    private UUID challengeId;
    private UUID communityId;
}
