package com.studywithus.community.application;

import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.MemberStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveCommunityMemberUseCase implements UseCase<RemoveCommunityMemberCommand, Void> {

    private final CommunityMemberRepository communityMemberRepository;

    @Override
    @Transactional
    public Void execute(RemoveCommunityMemberCommand command) {
        CommunityMember actor = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.actorUserId())
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Actor is not an active member of this community"));

        if (!actor.isAdmin()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Only admins or owner can remove members from the community");
        }

        if (command.actorUserId().equals(command.targetUserId())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "USE_LEAVE_ENDPOINT", "Use the leave endpoint to leave the community");
        }

        CommunityMember target = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.targetUserId())
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_MEMBER_NOT_FOUND", "Target user is not an active member of this community"));

        if (target.isOwner()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "CANNOT_REMOVE_OWNER", "The community owner cannot be removed");
        }

        if (actor.getRole() == CommunityRole.ADMIN && target.getRole() == CommunityRole.ADMIN) {
            throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Admins cannot remove other admins");
        }

        target.remove();
        communityMemberRepository.save(target);
        return null;
    }
}
