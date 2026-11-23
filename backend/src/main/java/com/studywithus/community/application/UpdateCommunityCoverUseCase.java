package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.media.application.MediaAssetResult;
import com.studywithus.media.application.ValidateMediaOwnershipUseCase;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCommunityCoverUseCase {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ValidateMediaOwnershipUseCase validateMediaOwnershipUseCase;

    @Transactional
    public CommunityResult execute(UUID communityId, UUID userId, UUID mediaId) {
        Community community = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(CommunityMember::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only community admins or owners can update cover"));

        if (!member.isAdmin()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only community admins or owners can update cover");
        }

        MediaAssetResult media = validateMediaOwnershipUseCase.validateAndResolveByMediaId(
                mediaId, userId, MediaPurpose.COMMUNITY_COVER
        );

        community.setImageUrl(media.url());
        community.setUpdatedAt(Instant.now());
        Community saved = communityRepository.save(community);
        long membersCount = communityMemberRepository.countActiveMembers(communityId);
        return CommunityResult.from(saved, membersCount, member.getRole());
    }
}
