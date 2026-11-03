package com.studywithus.identity.application;

import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.gamification.domain.BadgeRepository;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.UserBadge;
import com.studywithus.gamification.domain.UserStreak;
import com.studywithus.gamification.domain.UserStreakRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserProfileUseCase implements UseCase<UUID, UserProfileResult> {

    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CheckinRepository checkinRepository;
    private final UserStreakRepository userStreakRepository;
    private final BadgeRepository badgeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResult execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        long currentPoints = pointTransactionRepository.calculateTotalPointsByUser(userId);
        int activeCommunitiesCount = communityMemberRepository.findActiveCommunityIdsByUserId(userId).size();
        long checkinsCount = checkinRepository.countActiveByUserId(userId);
        UserStreak streak = userStreakRepository.findByUserId(userId).orElseGet(() -> UserStreak.empty(userId));
        List<UserBadge> badges = badgeRepository.findUserBadges(userId);

        return UserProfileResult.from(
                user,
                currentPoints,
                activeCommunitiesCount,
                checkinsCount,
                streak.getCurrentStreak(),
                streak.getLongestStreak(),
                badges
        );
    }
}
