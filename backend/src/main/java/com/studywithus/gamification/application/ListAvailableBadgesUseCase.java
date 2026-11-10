package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.Badge;
import com.studywithus.gamification.domain.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAvailableBadgesUseCase {

    private final BadgeRepository badgeRepository;

    @Transactional(readOnly = true)
    public List<Badge> execute() {
        return badgeRepository.findAllBadges();
    }
}
