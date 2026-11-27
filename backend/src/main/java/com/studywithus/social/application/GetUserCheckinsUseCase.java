package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserCheckinsUseCase {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final CheckinRepository checkinRepository;
    private final FeedItemHydrator feedItemHydrator;

    @Transactional(readOnly = true)
    public List<FeedItemResult> execute(UUID targetUserId, UUID callerId, int limit) {
        boolean isSelf = callerId != null && callerId.equals(targetUserId);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (!isSelf && !targetUser.isActive()) {
            throw new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }

        if (!isSelf && targetUser.isPrivate()) {
            boolean isFollowing = callerId != null && followRepository.existsByFollowerIdAndFollowingId(callerId, targetUserId);
            if (!isFollowing) {
                throw new DomainException(HttpStatus.FORBIDDEN, "PRIVATE_PROFILE", "This profile is private");
            }
        }

        int clampedLimit = limit > 0 ? Math.min(limit, 50) : 20;
        List<Checkin> checkins = checkinRepository.findUserCheckins(targetUserId, isSelf, clampedLimit);
        return feedItemHydrator.toCursorResult(checkins, clampedLimit, callerId).items();
    }
}
