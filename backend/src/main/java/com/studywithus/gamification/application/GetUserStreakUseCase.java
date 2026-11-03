package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.UserStreak;
import com.studywithus.gamification.domain.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserStreakUseCase {

    private final UserStreakRepository userStreakRepository;

    @Transactional(readOnly = true)
    public UserStreakResult execute(UUID userId) {
        UserStreak streak = userStreakRepository.findByUserId(userId)
                .orElseGet(() -> UserStreak.empty(userId));
        return UserStreakResult.from(streak);
    }
}
