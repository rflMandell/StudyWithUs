package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveCommunityUseCase implements UseCase<LeaveCommunityCommand, Void> {

    private final CommunityMemberRepository communityMemberRepository;

    @Override
    @Transactional
    public Void execute(LeaveCommunityCommand command) {
        CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.userId())
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_A_MEMBER", "User is not an active member of this community"));

        if (member.isOwner()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "OWNER_CANNOT_LEAVE", "Community owner cannot leave without transferring ownership");
        }

        member.leave();
        communityMemberRepository.save(member);
        return null;
    }
}
