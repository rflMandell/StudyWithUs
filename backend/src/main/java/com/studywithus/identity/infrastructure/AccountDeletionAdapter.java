package com.studywithus.identity.infrastructure;

import com.studywithus.identity.application.AccountDeletionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class AccountDeletionAdapter implements AccountDeletionPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void deleteUserPermanently(UUID userId) {
        Set<String> tables = loadExistingTables();

        // 1. Comment likes & comments
        if (hasTables(tables, "comment_likes", "comments", "checkins")) {
            exec("DELETE FROM comment_likes WHERE user_id = :userId OR comment_id IN (" +
                    "SELECT id FROM comments WHERE author_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId))", userId);
        }
        if (hasTables(tables, "comments", "checkins")) {
            exec("UPDATE comments SET parent_comment_id = NULL WHERE author_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId)", userId);
            exec("DELETE FROM comments WHERE parent_comment_id IS NOT NULL AND (author_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId))", userId);
            exec("DELETE FROM comments WHERE author_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId)", userId);
        }

        // 2. Checkin likes, media, point transactions, checkins
        if (hasTables(tables, "checkin_likes", "checkins")) {
            exec("DELETE FROM checkin_likes WHERE user_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId)", userId);
        }
        if (hasTables(tables, "checkin_media", "checkins")) {
            exec("DELETE FROM checkin_media WHERE checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId)", userId);
        }
        if (hasTables(tables, "point_transactions", "checkins")) {
            exec("UPDATE point_transactions SET reversal_id = NULL WHERE user_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId)", userId);
            exec("DELETE FROM point_transactions WHERE user_id = :userId OR checkin_id IN (SELECT id FROM checkins WHERE user_id = :userId)", userId);
        }
        if (hasTables(tables, "checkins")) {
            exec("DELETE FROM checkins WHERE user_id = :userId", userId);
        }

        // 3. Rankings, follows, communities membership
        if (hasTables(tables, "ranking_entries")) {
            exec("DELETE FROM ranking_entries WHERE user_id = :userId", userId);
        }
        if (hasTables(tables, "follows")) {
            exec("DELETE FROM follows WHERE follower_id = :userId OR following_id = :userId", userId);
        }
        if (hasTables(tables, "community_members")) {
            exec("DELETE FROM community_members WHERE user_id = :userId", userId);
        }
        if (hasTables(tables, "community_join_requests")) {
            exec("DELETE FROM community_join_requests WHERE user_id = :userId", userId);
            exec("UPDATE community_join_requests SET reviewed_by = NULL WHERE reviewed_by = :userId", userId);
        }

        // 4. Challenges & badges
        if (hasTables(tables, "user_badges", "challenges")) {
            exec("UPDATE user_badges SET challenge_id = NULL WHERE challenge_id IN (SELECT id FROM challenges WHERE creator_id = :userId)", userId);
        }
        if (hasTables(tables, "user_badges")) {
            exec("DELETE FROM user_badges WHERE user_id = :userId", userId);
        }
        if (hasTables(tables, "challenge_participants")) {
            exec("DELETE FROM challenge_participants WHERE user_id = :userId", userId);
        }
        if (hasTables(tables, "challenges")) {
            exec("DELETE FROM challenges WHERE creator_id = :userId", userId);
        }

        // 5. Streaks, XP, cosmetics, leagues, analytics
        deleteByUserIdIfPresent(tables, "user_streaks", "user_id", userId);
        deleteByUserIdIfPresent(tables, "community_streaks", "user_id", userId);
        deleteByUserIdIfPresent(tables, "user_levels", "user_id", userId);
        deleteByUserIdIfPresent(tables, "xp_transactions", "user_id", userId);
        deleteByUserIdIfPresent(tables, "user_cosmetics", "user_id", userId);
        deleteByUserIdIfPresent(tables, "league_participants", "user_id", userId);
        deleteByUserIdIfPresent(tables, "user_league_states", "user_id", userId);
        deleteByUserIdIfPresent(tables, "league_season_history", "user_id", userId);
        deleteByUserIdIfPresent(tables, "study_activity_records", "user_id", userId);
        deleteByUserIdIfPresent(tables, "study_daily_aggregates", "user_id", userId);
        deleteByUserIdIfPresent(tables, "study_subject_aggregates", "user_id", userId);

        // 6. Media, notifications, auth, subscriptions
        deleteByUserIdIfPresent(tables, "media_assets", "owner_id", userId);
        deleteByUserIdIfPresent(tables, "notifications", "user_id", userId);
        deleteByUserIdIfPresent(tables, "notification_preferences", "user_id", userId);
        deleteByUserIdIfPresent(tables, "device_tokens", "user_id", userId);
        deleteByUserIdIfPresent(tables, "refresh_tokens", "user_id", userId);
        deleteByUserIdIfPresent(tables, "oauth_accounts", "user_id", userId);
        deleteByUserIdIfPresent(tables, "user_credentials", "user_id", userId);
        deleteByUserIdIfPresent(tables, "subscriptions", "user_id", userId);

        // 7. Moderation & audit logs
        if (hasTables(tables, "reports")) {
            exec("DELETE FROM reports WHERE reporter_id = :userId", userId);
            exec("UPDATE reports SET reviewed_by = NULL WHERE reviewed_by = :userId", userId);
        }
        if (hasTables(tables, "audit_logs")) {
            exec("UPDATE audit_logs SET actor_id = NULL WHERE actor_id = :userId", userId);
        }

        // 8. Owned communities and their dependents
        if (hasTables(tables, "communities")) {
            if (hasTables(tables, "user_badges")) {
                exec("UPDATE user_badges SET community_id = NULL WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "comment_likes", "comments", "checkins")) {
                exec("DELETE FROM comment_likes WHERE comment_id IN (SELECT id FROM comments WHERE checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)))", userId);
            }
            if (hasTables(tables, "comments", "checkins")) {
                exec("UPDATE comments SET parent_comment_id = NULL WHERE checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId))", userId);
                exec("DELETE FROM comments WHERE checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId))", userId);
            }
            if (hasTables(tables, "checkin_likes", "checkins")) {
                exec("DELETE FROM checkin_likes WHERE checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId))", userId);
            }
            if (hasTables(tables, "checkin_media", "checkins")) {
                exec("DELETE FROM checkin_media WHERE checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId))", userId);
            }
            if (hasTables(tables, "point_transactions", "checkins")) {
                exec("UPDATE point_transactions SET reversal_id = NULL WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId) OR checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId))", userId);
                exec("DELETE FROM point_transactions WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId) OR checkin_id IN (SELECT id FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId))", userId);
            }
            if (hasTables(tables, "checkins")) {
                exec("DELETE FROM checkins WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "community_members")) {
                exec("DELETE FROM community_members WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "community_join_requests")) {
                exec("DELETE FROM community_join_requests WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "community_streaks")) {
                exec("DELETE FROM community_streaks WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "community_free_days")) {
                exec("DELETE FROM community_free_days WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "ranking_periods")) {
                exec("DELETE FROM ranking_periods WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            if (hasTables(tables, "ranking_configurations")) {
                exec("DELETE FROM ranking_configurations WHERE community_id IN (SELECT id FROM communities WHERE owner_id = :userId)", userId);
            }
            exec("DELETE FROM communities WHERE owner_id = :userId", userId);
        }

        // 9. Finally delete user
        exec("DELETE FROM users WHERE id = :userId", userId);
    }

    private Set<String> loadExistingTables() {
        List<?> raw = entityManager.createNativeQuery(
                "SELECT LOWER(table_name) FROM information_schema.tables"
        ).getResultList();
        Set<String> result = new HashSet<>();
        for (Object item : raw) {
            if (item != null) {
                result.add(item.toString().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private boolean hasTables(Set<String> tables, String... required) {
        for (String name : required) {
            if (!tables.contains(name)) {
                return false;
            }
        }
        return true;
    }

    private void deleteByUserIdIfPresent(Set<String> tables, String table, String column, UUID userId) {
        if (tables.contains(table)) {
            exec("DELETE FROM " + table + " WHERE " + column + " = :userId", userId);
        }
    }

    private void exec(String sql, UUID userId) {
        entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .executeUpdate();
    }
}
