package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.CommunityStreak;
import com.studywithus.gamification.domain.CommunityStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCommunityStreakUseCase {

    private final CommunityStreakRepository communityStreakRepository;

    @Transactional(readOnly = true)
    public CommunityStreakResult execute(UUID communityId, UUID userId) {
        CommunityStreak streak = communityStreakRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseGet(() -> CommunityStreak.empty(communityId, userId));

        return new CommunityStreakResult(
                streak.getCommunityId(),
                streak.getUserId(),
                streak.getCurrentStreak(),
                streak.getLongestStreak(),
                streak.getLastActivityDate()
        );
    }

    public record CommunityStreakResult(
            UUID communityId,
            UUID userId,
            int currentStreak,
            int longestStreak,
            LocalDate lastActivityDate
    ) {}
}
