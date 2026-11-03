package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserBadgesUseCase {

    private final BadgeRepository badgeRepository;

    @Transactional(readOnly = true)
    public List<UserBadgeResult> execute(UUID userId) {
        return badgeRepository.findUserBadges(userId).stream()
                .map(UserBadgeResult::from)
                .toList();
    }
}
