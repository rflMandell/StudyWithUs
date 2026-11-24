package com.studywithus.admin.infrastructure;

import com.studywithus.admin.application.AdminUser360Result;
import com.studywithus.admin.application.AdminUserFilter;
import com.studywithus.admin.application.AdminUserListItem;
import com.studywithus.admin.application.AdminUserQueryPort;
import com.studywithus.community.infrastructure.CommunityJpaEntity;
import com.studywithus.community.infrastructure.CommunityMemberJpaEntity;
import com.studywithus.identity.infrastructure.UserJpaEntity;
import com.studywithus.moderation.infrastructure.ReportJpaEntity;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.subscription.infrastructure.SubscriptionJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class AdminUserQueryAdapter implements AdminUserQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PagedResult<AdminUserListItem> findUsers(AdminUserFilter filter, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Data query
        CriteriaQuery<UserJpaEntity> dataQuery = cb.createQuery(UserJpaEntity.class);
        Root<UserJpaEntity> dataRoot = dataQuery.from(UserJpaEntity.class);
        List<Predicate> dataPredicates = buildPredicates(cb, dataQuery, dataRoot, filter);
        dataQuery.where(dataPredicates.toArray(Predicate[]::new));
        dataQuery.orderBy(cb.desc(dataRoot.get("createdAt")));

        List<UserJpaEntity> entities = entityManager.createQuery(dataQuery)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<UserJpaEntity> countRoot = countQuery.from(UserJpaEntity.class);
        List<Predicate> countPredicates = buildPredicates(cb, countQuery, countRoot, filter);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(Predicate[]::new));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Batch-resolve active PREMIUM subscriptions
        Set<UUID> premiumUserIds = resolvePremiumUserIds(entities);

        List<AdminUserListItem> items = entities.stream()
                .map(entity -> new AdminUserListItem(
                        entity.toDomain(),
                        premiumUserIds.contains(entity.getId()) ? "PREMIUM" : "FREE"
                ))
                .toList();

        return PagedResult.of(items, page, size, total);
    }

    @Override
    public List<AdminUser360Result.CommunitySummary> findUserCommunities(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT c.id, c.name, m.role, m.status, m.joinedAt " +
                                "FROM CommunityMemberJpaEntity m " +
                                "JOIN CommunityJpaEntity c ON c.id = m.id.communityId " +
                                "WHERE m.id.userId = :userId AND c.deletedAt IS NULL " +
                                "ORDER BY m.joinedAt DESC",
                        Object[].class
                )
                .setParameter("userId", userId)
                .getResultList();

        return rows.stream()
                .map(r -> new AdminUser360Result.CommunitySummary(
                        (UUID) r[0],
                        (String) r[1],
                        r[2].toString(),
                        r[3].toString(),
                        (Instant) r[4]
                ))
                .toList();
    }

    @Override
    public List<AdminUser360Result.ReportSummary> findUserReports(UUID userId) {
        List<ReportJpaEntity> reports = entityManager.createQuery(
                        "SELECT r FROM ReportJpaEntity r " +
                                "WHERE r.targetId = :userId OR r.reporterId = :userId " +
                                "ORDER BY r.createdAt DESC",
                        ReportJpaEntity.class
                )
                .setParameter("userId", userId)
                .setMaxResults(20)
                .getResultList();

        return reports.stream()
                .map(r -> new AdminUser360Result.ReportSummary(
                        r.getId(),
                        r.getTargetType().name(),
                        r.getTargetId(),
                        r.getReason(),
                        r.getStatus().name(),
                        userId.equals(r.getReporterId()),
                        r.getCreatedAt()
                ))
                .toList();
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<UserJpaEntity> root,
            AdminUserFilter filter
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter == null) {
            return predicates;
        }
        if (filter.status() != null) {
            predicates.add(cb.equal(root.get("status"), filter.status()));
        }
        if (filter.globalRole() != null) {
            predicates.add(cb.equal(root.get("globalRole"), filter.globalRole()));
        }
        if (filter.createdFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
        }
        if (filter.createdTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
        }
        if (filter.search() != null && !filter.search().isBlank()) {
            String pattern = "%" + filter.search().trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern)
            ));
        }
        if (filter.plan() != null && !filter.plan().isBlank()) {
            String normalizedPlan = filter.plan().trim().toUpperCase(Locale.ROOT);
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<SubscriptionJpaEntity> subRoot = sub.from(SubscriptionJpaEntity.class);
            sub.select(subRoot.get("userId"))
                    .where(
                            cb.equal(subRoot.get("userId"), root.get("id")),
                            cb.equal(subRoot.get("status"), "ACTIVE")
                    );
            if ("PREMIUM".equals(normalizedPlan)) {
                predicates.add(cb.exists(sub));
            } else if ("FREE".equals(normalizedPlan)) {
                predicates.add(cb.not(cb.exists(sub)));
            }
        }
        return predicates;
    }

    private Set<UUID> resolvePremiumUserIds(List<UserJpaEntity> users) {
        if (users.isEmpty()) {
            return Set.of();
        }
        List<UUID> ids = users.stream().map(UserJpaEntity::getId).toList();
        List<UUID> activeSubs = entityManager.createQuery(
                        "SELECT s.userId FROM SubscriptionJpaEntity s WHERE s.userId IN :ids AND s.status = 'ACTIVE'",
                        UUID.class
                )
                .setParameter("ids", ids)
                .getResultList();
        return new HashSet<>(activeSubs);
    }
}
