package com.studywithus.checkin.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinLikeRepository;
import com.studywithus.checkin.domain.CheckinLikedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleCheckinLikeUseCase implements UseCase<ToggleCheckinLikeCommand, LikeToggleResult> {

    private final CheckinRepository checkinRepository;
    private final CheckinLikeRepository checkinLikeRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LikeToggleResult execute(ToggleCheckinLikeCommand command) {
        Checkin checkin = checkinRepository.findById(command.checkinId())
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));

        if (checkin.getCommunityId() != null) {
            Community community = communityRepository.findById(checkin.getCommunityId())
                    .filter(Community::isActive)
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Comunidade não encontrada"));

            if (community.isPrivate() && !communityMemberRepository.isUserActiveMember(community.getId(), command.userId())) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_NOT_A_MEMBER",
                        "Apenas membros ativos da comunidade podem curtir este check-in"
                );
            }
        }

        boolean alreadyLiked = checkinLikeRepository.exists(checkin.getId(), command.userId());
        boolean liked;
        if (alreadyLiked) {
            checkinLikeRepository.delete(checkin.getId(), command.userId());
            liked = false;
        } else {
            checkinLikeRepository.save(checkin.getId(), command.userId());
            liked = true;
            eventPublisher.publishEvent(new CheckinLikedEvent(
                    checkin.getId(),
                    checkin.getUserId(),
                    command.userId(),
                    checkin.getCommunityId()
            ));
        }

        long likesCount = checkinLikeRepository.countByCheckinId(checkin.getId());
        return new LikeToggleResult(checkin.getId(), liked, likesCount);
    }
}
