package com.studywithus.social.application;

import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FollowRepository;
import com.studywithus.social.domain.FollowRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UnfollowUserUseCase {

    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final FeedTimelinePort feedTimelinePort;

    @Autowired
    public UnfollowUserUseCase(
            FollowRepository followRepository,
            FollowRequestRepository followRequestRepository,
            @Autowired(required = false) FeedTimelinePort feedTimelinePort
    ) {
        this.followRepository = followRepository;
        this.followRequestRepository = followRequestRepository;
        this.feedTimelinePort = feedTimelinePort;
    }

    @Transactional
    public FollowActionResult execute(UUID followerId, UUID targetUserId) {
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetUserId)) {
            followRepository.deleteByFollowerIdAndFollowingId(followerId, targetUserId);
        }
        followRequestRepository.deleteByRequesterIdAndTargetId(followerId, targetUserId);
        if (feedTimelinePort != null) {
            feedTimelinePort.invalidateUserTimeline(followerId);
        }
        return new FollowActionResult(FollowRelationshipStatus.NONE);
    }
}
