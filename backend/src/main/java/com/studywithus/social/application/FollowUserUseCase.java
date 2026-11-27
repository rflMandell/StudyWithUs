package com.studywithus.social.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.Follow;
import com.studywithus.social.domain.FollowRepository;
import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestRepository;
import com.studywithus.social.domain.FollowRequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class FollowUserUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final FeedTimelinePort feedTimelinePort;

    @Autowired
    public FollowUserUseCase(
            UserRepository userRepository,
            FollowRepository followRepository,
            FollowRequestRepository followRequestRepository,
            @Autowired(required = false) FeedTimelinePort feedTimelinePort
    ) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.followRequestRepository = followRequestRepository;
        this.feedTimelinePort = feedTimelinePort;
    }

    @Transactional
    public FollowActionResult execute(UUID followerId, UUID targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF", "Cannot follow yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetUserId)) {
            return new FollowActionResult(FollowRelationshipStatus.FOLLOWING);
        }

        if (!targetUser.isPrivate()) {
            followRepository.save(Follow.builder()
                    .followerId(followerId)
                    .followingId(targetUserId)
                    .createdAt(Instant.now())
                    .build());
            if (feedTimelinePort != null) {
                feedTimelinePort.invalidateUserTimeline(followerId);
            }
            return new FollowActionResult(FollowRelationshipStatus.FOLLOWING);
        }

        Optional<FollowRequest> existing = followRequestRepository.findByRequesterIdAndTargetId(followerId, targetUserId);
        if (existing.isPresent()) {
            FollowRequest request = existing.get();
            if (request.getStatus() == FollowRequestStatus.PENDING) {
                return new FollowActionResult(FollowRelationshipStatus.REQUESTED);
            }
            if (request.getStatus() == FollowRequestStatus.REJECTED) {
                request.reopen();
                followRequestRepository.save(request);
            }
            return new FollowActionResult(FollowRelationshipStatus.REQUESTED);
        }

        followRequestRepository.save(FollowRequest.create(followerId, targetUserId));
        return new FollowActionResult(FollowRelationshipStatus.REQUESTED);
    }
}
