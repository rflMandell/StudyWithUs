package com.studywithus.community.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
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
public class UpdateMemberRoleUseCase implements UseCase<UpdateMemberRoleCommand, CommunityMemberResult> {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    @Override
    @Transactional
    public CommunityMemberResult execute(UpdateMemberRoleCommand command) {
        CommunityMember actor = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.actorUserId())
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Actor is not an active member of this community"));

        if (!actor.isAdmin()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Only admins or owner can change member roles");
        }

        if (command.actorUserId().equals(command.targetUserId())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CANNOT_CHANGE_OWN_ROLE", "Members cannot change their own role directly");
        }

        CommunityMember target = communityMemberRepository.findByCommunityIdAndUserId(command.communityId(), command.targetUserId())
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_MEMBER_NOT_FOUND", "Target user is not an active member of this community"));

        if (actor.getRole() == CommunityRole.ADMIN) {
            if (target.getRole() == CommunityRole.OWNER || target.getRole() == CommunityRole.ADMIN) {
                throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Admins cannot modify roles of other admins or the owner");
            }
            if (command.newRole() == CommunityRole.OWNER || command.newRole() == CommunityRole.ADMIN) {
                throw new DomainException(HttpStatus.FORBIDDEN, "COMMUNITY_ACCESS_DENIED", "Admins can only promote or demote members to MODERATOR or MEMBER");
            }
        }

        if (actor.isOwner() && command.newRole() == CommunityRole.OWNER) {
            // Transfer ownership: actor becomes ADMIN, target becomes OWNER
            actor.changeRole(CommunityRole.ADMIN);
            target.changeRole(CommunityRole.OWNER);

            Community community = communityRepository.findById(command.communityId())
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));
            community.setOwnerId(command.targetUserId());
            communityRepository.save(community);

            communityMemberRepository.save(actor);
            CommunityMember savedTarget = communityMemberRepository.save(target);
            return CommunityMemberResult.from(savedTarget);
        }

        target.changeRole(command.newRole());
        CommunityMember savedTarget = communityMemberRepository.save(target);
        return CommunityMemberResult.from(savedTarget);
    }
}
