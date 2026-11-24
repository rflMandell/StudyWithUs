package com.studywithus.admin.infrastructure;

import com.studywithus.admin.domain.SystemLog;
import com.studywithus.admin.domain.SystemLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SystemLogRepositoryAdapter implements SystemLogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public SystemLog save(SystemLog log) {
        SystemLogJpaEntity entity = SystemLogJpaEntity.fromDomain(log);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemLog> findByFilter(
            String level,
            String module,
            String traceId,
            UUID userId,
            Integer statusCode,
            Instant from,
            Instant to,
            Instant cursorTimestamp,
            UUID cursorId,
            int limitPlusOne
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SystemLogJpaEntity> query = cb.createQuery(SystemLogJpaEntity.class);
        Root<SystemLogJpaEntity> root = query.from(SystemLogJpaEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        if (level != null && !level.isBlank()) {
            predicates.add(cb.equal(root.get("level"), level.trim().toUpperCase()));
        }
        if (module != null && !module.isBlank()) {
            predicates.add(cb.equal(root.get("module"), module.trim().toLowerCase()));
        }
        if (traceId != null && !traceId.isBlank()) {
            predicates.add(cb.equal(root.get("traceId"), traceId.trim()));
        }
        if (userId != null) {
            predicates.add(cb.equal(root.get("userId"), userId));
        }
        if (statusCode != null) {
            predicates.add(cb.equal(root.get("statusCode"), statusCode));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }
        if (cursorTimestamp != null && cursorId != null) {
            predicates.add(cb.or(
                    cb.lessThan(root.get("timestamp"), cursorTimestamp),
                    cb.and(
                            cb.equal(root.get("timestamp"), cursorTimestamp),
                            cb.lessThan(root.get("id"), cursorId)
                    )
            ));
        }

        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(cb.desc(root.get("timestamp")), cb.desc(root.get("id")));

        return entityManager.createQuery(query)
                .setMaxResults(limitPlusOne)
                .getResultList()
                .stream()
                .map(SystemLogJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemLog> findByTraceIdOrderByTimestampAsc(String traceId) {
        return entityManager.createQuery(
                        "SELECT s FROM SystemLogJpaEntity s WHERE s.traceId = :traceId ORDER BY s.timestamp ASC, s.id ASC",
                        SystemLogJpaEntity.class
                )
                .setParameter("traceId", traceId)
                .getResultList()
                .stream()
                .map(SystemLogJpaEntity::toDomain)
                .toList();
    }
}
