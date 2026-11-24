package com.studywithus.admin.infrastructure;

import com.studywithus.admin.domain.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Configurable retention policy for system_logs to prevent unbounded table growth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogRetentionJob {

    private final SystemLogRepository systemLogRepository;

    @Value("${app.admin.system-logs.retention-enabled:true}")
    private boolean retentionEnabled;

    @Value("${app.admin.system-logs.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.admin.system-logs.retention-cron:0 0 3 * * *}")
    public int purgeExpiredLogs() {
        if (!retentionEnabled || retentionDays <= 0) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = systemLogRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("SystemLogRetentionJob purged {} logs older than {} days (cutoff={})", deleted, retentionDays, cutoff);
        }
        return deleted;
    }
}
