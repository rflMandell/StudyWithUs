package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCheckinDetailsUseCase implements UseCase<UUID, CheckinResult> {

    private final CheckinRepository checkinRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

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

        return CheckinResult.fromDomain(checkin);
    }
}
