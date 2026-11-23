package com.studywithus.league.infrastructure;

import com.studywithus.league.application.CloseLeagueSeasonUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueSeasonScheduler {

    private final CloseLeagueSeasonUseCase closeLeagueSeasonUseCase;

    @Scheduled(cron = "${app.league.season-close-cron:0 0 0 * * MON}", zone = "UTC")
    public void closeWeeklyLeagueSeason() {
        log.info("Running scheduled weekly league season close job");
        closeLeagueSeasonUseCase.execute();
    }
}
