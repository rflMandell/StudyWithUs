package com.studywithus.social.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FollowRepository;
import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetFollowStatusUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;

    @Transactional(readOnly = true)
    public FollowStatusResult execute(UUID currentUserId, UUID targetUserId) {
        boolean isSelf = currentUserId.equals(targetUserId);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (!isSelf && !targetUser.isActive()) {
            throw new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }

        long followersCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        FollowRelationshipStatus status;
        if (isSelf) {
            status = FollowRelationshipStatus.SELF;
        } else if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            status = FollowRelationshipStatus.FOLLOWING;
        } else if (followRequestRepository.findByRequesterIdAndTargetId(currentUserId, targetUserId)
                .filter(FollowRequest::isPending)
                .isPresent()) {
            status = FollowRelationshipStatus.REQUESTED;
        } else {
            status = FollowRelationshipStatus.NONE;
        }

        return new FollowStatusResult(status, followersCount, followingCount);
    }
}
