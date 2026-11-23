package com.studywithus.ranking.application;

import com.studywithus.ranking.domain.RankingConfiguration;
import com.studywithus.ranking.domain.RankingConfigurationRepository;
import com.studywithus.ranking.domain.RankingPeriod;
import com.studywithus.ranking.domain.RankingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingPeriodManager {

    private final RankingConfigurationRepository configurationRepository;
    private final RankingPeriodRepository periodRepository;

    @Transactional
    public RankingPeriod getOrCreateActivePeriod(UUID communityId, String timezone) {
        Optional<RankingPeriod> activeOpt = periodRepository.findActiveByCommunityId(communityId);
        if (activeOpt.isPresent()) {
            RankingPeriod active = activeOpt.get();
            if (!active.isExpired()) {
                return active;
            }
            active.close();
            periodRepository.save(active);
        }

        RankingConfiguration config = configurationRepository.findByCommunityId(communityId)
                .orElseGet(() -> configurationRepository.save(
                        RankingConfiguration.createDefault(communityId, timezone)
                ));

        RankingPeriod newPeriod = RankingPeriod.createCurrentWeekly(communityId, config.getId());
        return periodRepository.save(newPeriod);
    }
}
