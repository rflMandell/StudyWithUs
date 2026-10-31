package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_join_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityJoinRequestJpaEntity {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityJoinRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    public static CommunityJoinRequestJpaEntity fromDomain(CommunityJoinRequest domain) {
        return CommunityJoinRequestJpaEntity.builder()
                .id(domain.getId())
                .communityId(domain.getCommunityId())
                .userId(domain.getUserId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .reviewedAt(domain.getReviewedAt())
                .reviewedBy(domain.getReviewedBy())
                .build();
    }

    public CommunityJoinRequest toDomain() {
        return CommunityJoinRequest.builder()
                .id(this.id)
                .communityId(this.communityId)
                .userId(this.userId)
                .status(this.status)
                .createdAt(this.createdAt)
                .reviewedAt(this.reviewedAt)
                .reviewedBy(this.reviewedBy)
                .build();
    }
}
