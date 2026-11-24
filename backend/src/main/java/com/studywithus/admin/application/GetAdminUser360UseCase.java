package com.studywithus.admin.application;

import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import com.studywithus.gamification.domain.UserStreak;
import com.studywithus.gamification.domain.UserStreakRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserCredential;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.UserLeagueState;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionRepository;
import com.studywithus.subscription.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetAdminUser360UseCase {

    private final UserRepository userRepository;
    private final AdminUserQueryPort adminUserQueryPort;
    private final SubscriptionRepository subscriptionRepository;
    private final UserStreakRepository userStreakRepository;
    private final UserLevelRepository userLevelRepository;
    private final LeagueRepository leagueRepository;

    @Transactional(readOnly = true)
    public AdminUser360Result execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        boolean totpEnabled = userRepository.findCredentialByUserId(userId)
                .map(UserCredential::isTotpEnabled)
                .orElse(false);

        Subscription subscription = subscriptionRepository.findByUserId(userId).orElse(null);
        boolean isPremium = subscription != null && subscription.getStatus() == SubscriptionStatus.ACTIVE;
        String plan = isPremium ? "PREMIUM" : "FREE";
        String subscriptionStatus = subscription != null ? subscription.getStatus().name() : "NONE";

        UserStreak streak = userStreakRepository.findByUserId(userId).orElse(null);
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int longestStreak = streak != null ? streak.getLongestStreak() : 0;

        UserLevel userLevel = userLevelRepository.findByUserId(userId).orElse(null);
        int level = userLevel != null ? userLevel.getCurrentLevel() : 1;
        long totalXp = userLevel != null ? userLevel.getTotalXp() : 0L;

        String leagueDivision = leagueRepository.findUserState(userId)
                .map(UserLeagueState::getCurrentDivision)
                .map(Enum::name)
                .orElse("NONE");

        List<AdminUser360Result.CommunitySummary> communities = adminUserQueryPort.findUserCommunities(userId);
        List<AdminUser360Result.ReportSummary> reports = adminUserQueryPort.findUserReports(userId);

        return new AdminUser360Result(
                user,
                totpEnabled,
                plan,
                subscriptionStatus,
                subscription != null ? subscription.getExpiresAt() : null,
                currentStreak,
                longestStreak,
                level,
                totalXp,
                leagueDivision,
                communities,
                reports
        );
    }
}
