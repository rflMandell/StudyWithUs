package com.studywithus.community.domain;

import com.studywithus.shared.domain.DomainException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityJoinRequest {
    private UUID id;
    private UUID communityId;
    private UUID userId;
    private CommunityJoinRequestStatus status;
    private Instant createdAt;
    private Instant reviewedAt;
    private UUID reviewedBy;

    public static CommunityJoinRequest create(UUID communityId, UUID userId) {
        return CommunityJoinRequest.builder()
                .id(UUID.randomUUID())
                .communityId(communityId)
                .userId(userId)
                .status(CommunityJoinRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    public boolean isPending() {
        return status == CommunityJoinRequestStatus.PENDING;
    }

    public void approve(UUID reviewerId) {
        ensurePending();
        this.status = CommunityJoinRequestStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    public void reject(UUID reviewerId) {
        ensurePending();
        this.status = CommunityJoinRequestStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = Instant.now();
    }

    public void cancel() {
        ensurePending();
        this.status = CommunityJoinRequestStatus.CANCELLED;
        this.reviewedAt = Instant.now();
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new DomainException(
                    HttpStatus.BAD_REQUEST,
                    "JOIN_REQUEST_NOT_PENDING",
                    "Join request is already " + status
            );
        }
    }
}
