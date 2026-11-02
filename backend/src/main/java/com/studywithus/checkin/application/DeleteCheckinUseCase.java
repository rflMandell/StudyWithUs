package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCheckinUseCase implements UseCase<DeleteCheckinCommand, Void> {

    private final CheckinRepository checkinRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Void execute(DeleteCheckinCommand command) {
        Checkin checkin = checkinRepository.findById(command.checkinId())
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Checkin not found"));

        boolean isAuthor = checkin.isAuthor(command.actorUserId());

        if (!isAuthor) {
            // Check if actor is an admin/moderator of the community
            CommunityMember actorMember = communityMemberRepository.findByCommunityIdAndUserId(
                    checkin.getCommunityId(),
                    command.actorUserId()
            ).filter(CommunityMember::isActive)
             .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "CHECKIN_ACCESS_DENIED", "User is not authorized to delete this checkin"));

            if (!actorMember.isModeratorOrHigher()) {
                throw new DomainException(HttpStatus.FORBIDDEN, "CHECKIN_ACCESS_DENIED", "Only the author or community moderators/admins can delete this checkin");
            }
        }

        checkin.delete();
        checkinRepository.save(checkin);

        eventPublisher.publishEvent(new CheckinDeletedEvent(
                checkin.getId(),
                checkin.getUserId(),
                checkin.getCommunityId(),
                command.actorUserId()
        ));

        return null;
    }
}
