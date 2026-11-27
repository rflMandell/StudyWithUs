package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestStatus;
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
@Table(name = "follow_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequestJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FollowRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FollowRequest toDomain() {
        return FollowRequest.builder()
                .id(id)
                .requesterId(requesterId)
                .targetId(targetId)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static FollowRequestJpaEntity fromDomain(FollowRequest request) {
        Instant now = Instant.now();
        return FollowRequestJpaEntity.builder()
                .id(request.getId() != null ? request.getId() : UUID.randomUUID())
                .requesterId(request.getRequesterId())
                .targetId(request.getTargetId())
                .status(request.getStatus() != null ? request.getStatus() : FollowRequestStatus.PENDING)
                .createdAt(request.getCreatedAt() != null ? request.getCreatedAt() : now)
                .updatedAt(request.getUpdatedAt() != null ? request.getUpdatedAt() : now)
                .build();
    }
}
