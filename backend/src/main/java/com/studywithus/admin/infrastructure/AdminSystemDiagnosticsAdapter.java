package com.studywithus.admin.infrastructure;

import com.studywithus.admin.application.AdminSystemDiagnosticsPort;
import com.studywithus.admin.application.SystemHealthResult;
import com.studywithus.admin.application.SystemInfoResult;
import com.studywithus.admin.application.SystemOverviewResult;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.moderation.domain.ReportRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class AdminSystemDiagnosticsAdapter implements AdminSystemDiagnosticsPort {

    private final DataSource dataSource;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final ObjectProvider<ObjectStoragePort> objectStoragePortProvider;
    private final ObjectProvider<Flyway> flywayProvider;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final Environment environment;
    private final ReportRepository reportRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String fallbackVersion;

    @Value("${app.git-commit:local}")
    private String commitSha;

    @Override
    public SystemInfoResult getSystemInfo() {
        BuildProperties buildProps = buildPropertiesProvider.getIfAvailable();
        String appName = buildProps != null ? buildProps.getName() : "studywithus-backend";
        String version = buildProps != null ? buildProps.getVersion() : fallbackVersion;

        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        if (profiles.isEmpty()) {
            profiles = Arrays.asList(environment.getDefaultProfiles());
        }

        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        SystemInfoResult.FlywayStatus flywayStatus = resolveFlywayStatus();

        return new SystemInfoResult(
                appName,
                version,
                commitSha,
                profiles,
                uptimeSeconds,
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"),
                flywayStatus
        );
    }

    @Override
    public SystemHealthResult getSystemHealth() {
        SystemHealthResult.DatabaseHealth dbHealth = checkDatabase();
        SystemHealthResult.RedisHealth redisHealth = checkRedis();
        SystemHealthResult.ObjectStorageHealth storageHealth = checkObjectStorage();
        SystemHealthResult.OutboxAndJobsHealth outboxHealth = checkOutboxAndJobs();

        String overall = "UP";
        if (!"UP".equals(dbHealth.status())) {
            overall = "DOWN";
        } else if (!"UP".equals(redisHealth.status()) || !"UP".equals(storageHealth.status())) {
            overall = "DEGRADED";
        }

        return new SystemHealthResult(
                overall,
                Instant.now(),
                dbHealth,
                redisHealth,
                storageHealth,
                outboxHealth
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SystemOverviewResult getSystemOverview() {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);

        long totalUsers = countQuery("SELECT COUNT(u) FROM UserJpaEntity u");
        long activeUsers = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.status = :status AND u.deletedAt IS NULL",
                        Long.class)
                .setParameter("status", UserStatus.ACTIVE)
                .getSingleResult();
        long suspendedUsers = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.status = :status",
                        Long.class)
                .setParameter("status", UserStatus.SUSPENDED)
                .getSingleResult();
        long newUsersLast24h = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.createdAt >= :since",
                        Long.class)
                .setParameter("since", since24h)
                .getSingleResult();
        long activeCommunities = countQuery("SELECT COUNT(c) FROM CommunityJpaEntity c WHERE c.deletedAt IS NULL");
        long checkinsLast24h = entityManager.createQuery(
                        "SELECT COUNT(c) FROM CheckinJpaEntity c WHERE c.createdAt >= :since AND c.deletedAt IS NULL",
                        Long.class)
                .setParameter("since", since24h)
                .getSingleResult();
        long activePremiumSubscriptions = countQuery(
                "SELECT COUNT(s) FROM SubscriptionJpaEntity s WHERE s.status = 'ACTIVE'");
        long pendingReports = reportRepository.countPending();

        return new SystemOverviewResult(
                totalUsers,
                activeUsers,
                suspendedUsers,
                newUsersLast24h,
                activeCommunities,
                checkinsLast24h,
                activePremiumSubscriptions,
                pendingReports
        );
    }

    private long countQuery(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    private SystemInfoResult.FlywayStatus resolveFlywayStatus() {
        Flyway flyway = flywayProvider.getIfAvailable();
        if (flyway == null) {
            return new SystemInfoResult.FlywayStatus(false, "N/A", 0, 0);
        }
        try {
            MigrationInfoService info = flyway.info();
            String current = info.current() != null && info.current().getVersion() != null
                    ? info.current().getVersion().getVersion()
                    : "none";
            return new SystemInfoResult.FlywayStatus(
                    true,
                    current,
                    info.applied().length,
                    info.pending().length
            );
        } catch (Exception e) {
            return new SystemInfoResult.FlywayStatus(true, "unknown", 0, 0);
        }
    }

    private SystemHealthResult.DatabaseHealth checkDatabase() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            long latency = System.currentTimeMillis() - start;

            int active = 0;
            int idle = 0;
            int total = 0;
            int max = 0;
            if (dataSource instanceof HikariDataSource hikari) {
                max = hikari.getMaximumPoolSize();
                HikariPoolMXBean mxBean = hikari.getHikariPoolMXBean();
                if (mxBean != null) {
                    active = mxBean.getActiveConnections();
                    idle = mxBean.getIdleConnections();
                    total = mxBean.getTotalConnections();
                }
            }
            return new SystemHealthResult.DatabaseHealth("UP", latency, active, idle, total, max, null);
        } catch (Exception e) {
            return new SystemHealthResult.DatabaseHealth(
                    "DOWN", System.currentTimeMillis() - start, 0, 0, 0, 0, e.getMessage()
            );
        }
    }

    private SystemHealthResult.RedisHealth checkRedis() {
        RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            return new SystemHealthResult.RedisHealth("DOWN", 0L, null, null, "RedisConnectionFactory not configured");
        }
        long start = System.currentTimeMillis();
        try (RedisConnection conn = factory.getConnection()) {
            String pong = conn.ping();
            long latency = System.currentTimeMillis() - start;
            if (pong == null) {
                return new SystemHealthResult.RedisHealth("DOWN", latency, null, null, "Redis PING returned null");
            }
            String usedMemory = null;
            try {
                Properties memInfo = conn.serverCommands().info("memory");
                if (memInfo != null) {
                    usedMemory = memInfo.getProperty("used_memory_human");
                }
            } catch (Exception ignored) {
            }
            Long dbSize = null;
            try {
                dbSize = conn.serverCommands().dbSize();
            } catch (Exception ignored) {
            }
            return new SystemHealthResult.RedisHealth("UP", latency, usedMemory, dbSize, null);
        } catch (Exception e) {
            return new SystemHealthResult.RedisHealth(
                    "DOWN", System.currentTimeMillis() - start, null, null, e.getMessage()
            );
        }
    }

    private SystemHealthResult.ObjectStorageHealth checkObjectStorage() {
        ObjectStoragePort port = objectStoragePortProvider.getIfAvailable();
        if (port == null) {
            return new SystemHealthResult.ObjectStorageHealth("DOWN", "NONE", "N/A", 0L, "ObjectStoragePort not configured");
        }
        try {
            ObjectStoragePort.StorageHealth health = port.checkHealth();
            return new SystemHealthResult.ObjectStorageHealth(
                    health.up() ? "UP" : "DOWN",
                    health.provider(),
                    health.bucketOrPath(),
                    health.latencyMs(),
                    health.error()
            );
        } catch (Exception e) {
            return new SystemHealthResult.ObjectStorageHealth("DOWN", "UNKNOWN", "N/A", 0L, e.getMessage());
        }
    }

    private SystemHealthResult.OutboxAndJobsHealth checkOutboxAndJobs() {
        long pending = 0;
        long failed = 0;
        try {
            Object pendingObj = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING'").getSingleResult();
            Object failedObj = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM outbox_events WHERE status = 'FAILED'").getSingleResult();
            pending = pendingObj instanceof Number n ? n.longValue() : 0L;
            failed = failedObj instanceof Number n ? n.longValue() : 0L;
        } catch (Exception ignored) {
            // outbox_events table may not exist in H2 create-drop test profile
        }

        List<SystemHealthResult.ScheduledJobStatus> jobs = new ArrayList<>();
        try {
            List<?> rows = entityManager.createQuery(
                            "SELECT s.status, s.updatedAt FROM LeagueSeasonJpaEntity s ORDER BY s.updatedAt DESC")
                    .setMaxResults(1)
                    .getResultList();
            if (!rows.isEmpty()) {
                Object[] row = (Object[]) rows.get(0);
                jobs.add(new SystemHealthResult.ScheduledJobStatus(
                        "weekly-league-season",
                        String.valueOf(row[0]),
                        row[1] instanceof Instant i ? i : null
                ));
            }
        } catch (Exception ignored) {
        }
        try {
            List<?> rows = entityManager.createQuery(
                            "SELECT p.status, p.createdAt FROM RankingPeriodJpaEntity p ORDER BY p.createdAt DESC")
                    .setMaxResults(1)
                    .getResultList();
            if (!rows.isEmpty()) {
                Object[] row = (Object[]) rows.get(0);
                jobs.add(new SystemHealthResult.ScheduledJobStatus(
                        "community-ranking-period",
                        String.valueOf(row[0]),
                        row[1] instanceof Instant i ? i : null
                ));
            }
        } catch (Exception ignored) {
        }

        String status = failed > 0 ? "DEGRADED" : "UP";
        return new SystemHealthResult.OutboxAndJobsHealth(status, pending, failed, jobs);
    }
}
