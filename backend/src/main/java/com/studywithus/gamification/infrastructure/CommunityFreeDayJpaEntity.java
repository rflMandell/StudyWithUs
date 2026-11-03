package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.CommunityFreeDay;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "community_free_days")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityFreeDayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CommunityFreeDay toDomain() {
        return CommunityFreeDay.builder()
                .id(id)
                .communityId(communityId)
                .dayOfWeek(dayOfWeek)
                .createdAt(createdAt)
                .build();
    }

    public static CommunityFreeDayJpaEntity fromDomain(CommunityFreeDay domain) {
        return CommunityFreeDayJpaEntity.builder()
                .id(domain.getId())
                .communityId(domain.getCommunityId())
                .dayOfWeek(domain.getDayOfWeek())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .build();
    }
}
