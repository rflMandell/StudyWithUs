package com.studywithus.challenge.infrastructure;

import com.studywithus.challenge.domain.ChallengeParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "challenge_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipantJpaEntity {

    @EmbeddedId
    private ChallengeParticipantId id;

    @Column(nullable = false)
    private int progress;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public static ChallengeParticipantJpaEntity fromDomain(ChallengeParticipant domain) {
        return ChallengeParticipantJpaEntity.builder()
                .id(new ChallengeParticipantId(domain.getChallengeId(), domain.getUserId()))
                .progress(domain.getProgress())
                .completedAt(domain.getCompletedAt())
                .joinedAt(domain.getJoinedAt() != null ? domain.getJoinedAt() : Instant.now())
                .build();
    }

    public ChallengeParticipant toDomain() {
        return ChallengeParticipant.builder()
                .challengeId(this.id.getChallengeId())
                .userId(this.id.getUserId())
                .progress(this.progress)
                .completedAt(this.completedAt)
                .joinedAt(this.joinedAt)
                .build();
    }
}
