package com.studywithus.ranking.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.ranking.domain.RankingEntry;
import com.studywithus.ranking.domain.RankingEntryRepository;
import com.studywithus.ranking.domain.RankingPeriod;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetUserCommunityRankingPositionUseCase {

    private final CommunityRepository communityRepository;
    private final RankingPeriodManager periodManager;
    private final RankingEntryRepository rankingEntryRepository;

    @Transactional(readOnly = true)
    public UserRankingPositionResult execute(UUID communityId, UUID userId) {
        Community community = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Community not found"));

        RankingPeriod period = periodManager.getOrCreateActivePeriod(communityId, community.getTimezone());

        Optional<RankingEntry> entryOpt = rankingEntryRepository.findByPeriodIdAndUserId(period.getId(), userId);

        if (entryOpt.isPresent()) {
            RankingEntry entry = entryOpt.get();
            return new UserRankingPositionResult(
                    communityId,
                    period.getId(),
                    userId,
                    entry.getPoints(),
                    entry.getPosition(),
                    entry.getStreakAtUpdate()
            );
        }

        return new UserRankingPositionResult(
                communityId,
                period.getId(),
                userId,
                0,
                0,
                0
        );
    }
}
