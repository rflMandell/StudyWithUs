package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "community_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityMemberJpaEntity {

    @EmbeddedId
    private CommunityMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "banned_at")
    private Instant bannedAt;

    public CommunityMember toDomain() {
        return CommunityMember.builder()
                .communityId(id.getCommunityId())
                .userId(id.getUserId())
                .role(role)
                .status(status)
                .joinedAt(joinedAt)
                .leftAt(leftAt)
                .bannedAt(bannedAt)
                .build();
    }

    public static CommunityMemberJpaEntity fromDomain(CommunityMember member) {
        return CommunityMemberJpaEntity.builder()
                .id(new CommunityMemberId(member.getCommunityId(), member.getUserId()))
                .role(member.getRole())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt() != null ? member.getJoinedAt() : Instant.now())
                .leftAt(member.getLeftAt())
                .bannedAt(member.getBannedAt())
                .build();
    }
}
