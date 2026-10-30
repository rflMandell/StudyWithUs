package com.studywithus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationDiscoveryTest {

    @Test
    @DisplayName("Flyway V1 migration script must exist and contain core tables defined in docs/04_DADOS_POSTGRESQL.md")
    void v1MigrationScriptIsValid() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_schema.sql");
        assertThat(resource.exists()).isTrue();

        try (InputStream is = resource.getInputStream()) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Core tables validation
            assertThat(sql)
                    .contains("CREATE TABLE users")
                    .contains("CREATE TABLE user_credentials")
                    .contains("CREATE TABLE oauth_accounts")
                    .contains("CREATE TABLE refresh_tokens")
                    .contains("CREATE TABLE follows")
                    .contains("CREATE TABLE communities")
                    .contains("CREATE TABLE community_members")
                    .contains("CREATE TABLE community_join_requests")
                    .contains("CREATE TABLE ranking_configurations")
                    .contains("CREATE TABLE ranking_periods")
                    .contains("CREATE TABLE ranking_entries")
                    .contains("CREATE TABLE checkins")
                    .contains("CREATE TABLE checkin_media")
                    .contains("CREATE TABLE comments")
                    .contains("CREATE TABLE point_transactions")
                    .contains("CREATE TABLE user_streaks")
                    .contains("CREATE TABLE community_streaks")
                    .contains("CREATE TABLE community_free_days")
                    .contains("CREATE TABLE challenges")
                    .contains("CREATE TABLE challenge_participants")
                    .contains("CREATE TABLE challenge_rewards")
                    .contains("CREATE TABLE badges")
                    .contains("CREATE TABLE user_badges")
                    .contains("CREATE TABLE notifications")
                    .contains("CREATE TABLE notification_preferences")
                    .contains("CREATE TABLE device_tokens")
                    .contains("CREATE TABLE reports")
                    .contains("CREATE TABLE audit_logs")
                    .contains("CREATE TABLE subscriptions")
                    .contains("CREATE TABLE subscription_events")
                    .contains("CREATE TABLE outbox_events");

            // Key indexes validation
            assertThat(sql)
                    .contains("idx_checkins_community_created")
                    .contains("idx_checkins_user_created")
                    .contains("idx_comments_checkin_created")
                    .contains("idx_community_members_user")
                    .contains("idx_community_members_community_status")
                    .contains("idx_ranking_entries_period_pos")
                    .contains("idx_notifications_user_created")
                    .contains("idx_point_transactions_user_created")
                    .contains("idx_outbox_status_created");
        }
    }
}
