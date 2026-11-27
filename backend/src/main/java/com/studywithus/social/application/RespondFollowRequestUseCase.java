package com.studywithus.social.application;

import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.Follow;
import com.studywithus.social.domain.FollowRepository;
import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RespondFollowRequestUseCase {

    private final FollowRequestRepository followRequestRepository;
    private final FollowRepository followRepository;
    private final FeedTimelinePort feedTimelinePort;

    @Autowired
    public RespondFollowRequestUseCase(
            FollowRequestRepository followRequestRepository,
            FollowRepository followRepository,
            @Autowired(required = false) FeedTimelinePort feedTimelinePort
    ) {
        this.followRequestRepository = followRequestRepository;
        this.followRepository = followRepository;
        this.feedTimelinePort = feedTimelinePort;
    }

    @Transactional
    public void execute(UUID requestId, UUID currentUserId, boolean accept) {
        FollowRequest request = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "FOLLOW_REQUEST_NOT_FOUND", "Follow request not found"));

        if (!request.getTargetId().equals(currentUserId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to respond to this follow request");
        }

        if (!request.isPending()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "FOLLOW_REQUEST_ALREADY_PROCESSED", "Follow request has already been processed");
        }

        if (accept) {
            request.accept();
            followRequestRepository.save(request);
            if (!followRepository.existsByFollowerIdAndFollowingId(request.getRequesterId(), request.getTargetId())) {
                followRepository.save(Follow.builder()
                        .followerId(request.getRequesterId())
                        .followingId(request.getTargetId())
                        .createdAt(Instant.now())
                        .build());
            }
            if (feedTimelinePort != null) {
                feedTimelinePort.invalidateUserTimeline(request.getRequesterId());
            }
        } else {
            request.reject();
            followRequestRepository.save(request);
        }
    }
}
