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
public class CommunityFreeDay {
    private UUID id;
    private UUID communityId;
    private int dayOfWeek;
    private Instant createdAt;
}
