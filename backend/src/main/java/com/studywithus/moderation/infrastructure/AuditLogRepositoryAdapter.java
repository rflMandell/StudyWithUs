package com.studywithus.moderation.infrastructure;

import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.shared.domain.PagedResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(UUID.randomUUID());
        }
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(Instant.now());
        }
        AuditLogJpaEntity entity = AuditLogJpaEntity.fromDomain(auditLog);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    public PagedResult<AuditLog> findAll(String targetType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditLogJpaEntity> resultPage = (targetType != null && !targetType.isBlank())
                ? repository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageRequest)
                : repository.findAllByOrderByCreatedAtDesc(pageRequest);

        return PagedResult.of(
                resultPage.getContent().stream().map(AuditLogJpaEntity::toDomain).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }

    @Override
    public PagedResult<AuditLog> findByFilters(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<AuditLogJpaEntity> dataQuery = cb.createQuery(AuditLogJpaEntity.class);
        Root<AuditLogJpaEntity> dataRoot = dataQuery.from(AuditLogJpaEntity.class);
        List<Predicate> dataPredicates = buildPredicates(cb, dataRoot, actorId, action, targetType, targetId, from, to);
        dataQuery.where(dataPredicates.toArray(Predicate[]::new));
        dataQuery.orderBy(cb.desc(dataRoot.get("createdAt")));

        List<AuditLog> items = entityManager.createQuery(dataQuery)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList()
                .stream()
                .map(AuditLogJpaEntity::toDomain)
                .toList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditLogJpaEntity> countRoot = countQuery.from(AuditLogJpaEntity.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, actorId, action, targetType, targetId, from, to);
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicates.toArray(Predicate[]::new));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return PagedResult.of(items, page, size, total);
    }

    @Override
    public List<AuditLog> findForExport(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to,
            int limit
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLogJpaEntity> query = cb.createQuery(AuditLogJpaEntity.class);
        Root<AuditLogJpaEntity> root = query.from(AuditLogJpaEntity.class);
        List<Predicate> predicates = buildPredicates(cb, root, actorId, action, targetType, targetId, from, to);
        query.where(predicates.toArray(Predicate[]::new));
        query.orderBy(cb.desc(root.get("createdAt")));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(AuditLogJpaEntity::toDomain)
                .toList();
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<AuditLogJpaEntity> root,
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (actorId != null) {
            predicates.add(cb.equal(root.get("actorId"), actorId));
        }
        if (action != null && !action.isBlank()) {
            predicates.add(cb.equal(root.get("action"), action.trim().toUpperCase()));
        }
        if (targetType != null && !targetType.isBlank()) {
            predicates.add(cb.equal(root.get("targetType"), targetType.trim().toUpperCase()));
        }
        if (targetId != null) {
            predicates.add(cb.equal(root.get("targetId"), targetId));
        }
        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return predicates;
    }
}
