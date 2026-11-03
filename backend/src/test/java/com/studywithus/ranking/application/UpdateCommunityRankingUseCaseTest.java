package com.studywithus.ranking.application;

import com.studywithus.ranking.domain.RankingEntry;
import com.studywithus.ranking.domain.RankingEntryRepository;
import com.studywithus.ranking.domain.RankingPeriod;
import com.studywithus.ranking.domain.RankingPeriodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCommunityRankingUseCaseTest {

    @Mock
    private RankingPeriodManager periodManager;

    @Mock
    private RankingEntryRepository rankingEntryRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UpdateCommunityRankingUseCase updateCommunityRankingUseCase;

    private final UUID communityId = UUID.randomUUID();
    private final UUID periodId = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();
    private final UUID userC = UUID.randomUUID();

    private RankingPeriod period;

    @BeforeEach
    void setUp() {
        period = RankingPeriod.builder()
                .id(periodId)
                .communityId(communityId)
                .status(RankingPeriodStatus.ACTIVE)
                .startsAt(Instant.now())
                .endsAt(Instant.now().plusSeconds(86400 * 7))
                .build();

        when(periodManager.getOrCreateActivePeriod(eq(communityId), any())).thenReturn(period);
    }

    @Test
    @DisplayName("Recalculates positions correctly: 1st B (200 pts), 2nd A (100 pts), 3rd C (50 pts)")
    void recalculatePositionsInDescendingOrder() {
        RankingEntry entryB = RankingEntry.builder()
                .rankingPeriodId(periodId)
                .userId(userB)
                .points(200)
                .position(0)
                .updatedAt(Instant.now())
                .build();

        RankingEntry entryA = RankingEntry.builder()
                .rankingPeriodId(periodId)
                .userId(userA)
                .points(100)
                .position(0)
                .updatedAt(Instant.now())
                .build();

        RankingEntry entryC = RankingEntry.builder()
                .rankingPeriodId(periodId)
                .userId(userC)
                .points(50)
                .position(0)
                .updatedAt(Instant.now())
                .build();

        when(rankingEntryRepository.findByPeriodIdAndUserId(periodId, userB))
                .thenReturn(Optional.of(entryB));
        when(rankingEntryRepository.findByPeriodIdOrderByPointsDesc(periodId))
                .thenReturn(new ArrayList<>(List.of(entryB, entryA, entryC)));

        updateCommunityRankingUseCase.execute(communityId, userB, 0);

        assertThat(entryB.getPosition()).isEqualTo(1);
        assertThat(entryA.getPosition()).isEqualTo(2);
        assertThat(entryC.getPosition()).isEqualTo(3);

        verify(rankingEntryRepository).saveAll(any());
    }
}
