package com.studywithus.social.domain;

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
public class FollowRequest {
    private UUID id;
    private UUID requesterId;
    private UUID targetId;
    private FollowRequestStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static FollowRequest create(UUID requesterId, UUID targetId) {
        Instant now = Instant.now();
        return FollowRequest.builder()
                .id(UUID.randomUUID())
                .requesterId(requesterId)
                .targetId(targetId)
                .status(FollowRequestStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public boolean isPending() {
        return status == FollowRequestStatus.PENDING;
    }

    public void accept() {
        this.status = FollowRequestStatus.ACCEPTED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        this.status = FollowRequestStatus.REJECTED;
        this.updatedAt = Instant.now();
    }

    public void reopen() {
        this.status = FollowRequestStatus.PENDING;
        this.updatedAt = Instant.now();
    }
}
