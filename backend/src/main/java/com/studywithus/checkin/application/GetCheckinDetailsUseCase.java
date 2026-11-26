package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinLikeRepository;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetCheckinDetailsUseCase implements UseCase<UUID, CheckinResult> {

    private final CheckinRepository checkinRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CheckinLikeRepository checkinLikeRepository;

    @Autowired
    public GetCheckinDetailsUseCase(
            CheckinRepository checkinRepository,
            CommunityRepository communityRepository,
            CommunityMemberRepository communityMemberRepository,
            @Autowired(required = false) CheckinLikeRepository checkinLikeRepository
    ) {
        this.checkinRepository = checkinRepository;
        this.communityRepository = communityRepository;
        this.communityMemberRepository = communityMemberRepository;
        this.checkinLikeRepository = checkinLikeRepository;
    }

    public GetCheckinDetailsUseCase(
            CheckinRepository checkinRepository,
            CommunityRepository communityRepository,
            CommunityMemberRepository communityMemberRepository
    ) {
        this(checkinRepository, communityRepository, communityMemberRepository, null);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckinResult execute(UUID checkinId) {
        return execute(checkinId, null);
    }

    @Transactional(readOnly = true)
    public CheckinResult execute(UUID checkinId, UUID callerId) {
        Checkin checkin = checkinRepository.findById(checkinId)
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Checkin not found"));

        Community community = communityRepository.findById(checkin.getCommunityId())
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        if (community.isPrivate()) {
            if (callerId == null || !communityMemberRepository.isUserActiveMember(community.getId(), callerId)) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_ACCESS_DENIED",
                        "Only active members can view check-ins of a private community"
                );
            }
        }

        long likesCount = checkinLikeRepository != null ? checkinLikeRepository.countByCheckinId(checkin.getId()) : 0L;
        boolean likedByCaller = checkinLikeRepository != null && callerId != null
                && checkinLikeRepository.exists(checkin.getId(), callerId);

        return CheckinResult.fromDomain(checkin, 0L, likesCount, likedByCaller);
    }
}
