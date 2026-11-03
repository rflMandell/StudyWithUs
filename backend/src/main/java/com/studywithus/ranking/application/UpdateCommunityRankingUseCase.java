package com.studywithus.ranking.application;

import com.studywithus.ranking.domain.RankingEntry;
import com.studywithus.ranking.domain.RankingEntryRepository;
import com.studywithus.ranking.domain.RankingPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateCommunityRankingUseCase {

    private final RankingPeriodManager periodManager;
    private final RankingEntryRepository rankingEntryRepository;

    @Transactional
    public void execute(UUID communityId, UUID userId, long pointsDelta) {
        RankingPeriod period = periodManager.getOrCreateActivePeriod(communityId, "UTC");

        RankingEntry entry = rankingEntryRepository.findByPeriodIdAndUserId(period.getId(), userId)
                .orElseGet(() -> RankingEntry.create(period.getId(), userId, 0));

        entry.addPoints(pointsDelta);
        rankingEntryRepository.save(entry);

        // Recalculate positions for all entries in the active period
        List<RankingEntry> entries = rankingEntryRepository.findByPeriodIdOrderByPointsDesc(period.getId());

        int currentPosition = 1;
        for (RankingEntry e : entries) {
            e.updatePosition(currentPosition++);
        }

        rankingEntryRepository.saveAll(entries);
        log.info("Recalculated ranking for communityId: {}, periodId: {}, total entries: {}", communityId, period.getId(), entries.size());
    }
}
