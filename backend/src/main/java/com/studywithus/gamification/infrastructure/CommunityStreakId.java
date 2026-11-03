package com.studywithus.gamification.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CommunityStreakId implements Serializable {

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
