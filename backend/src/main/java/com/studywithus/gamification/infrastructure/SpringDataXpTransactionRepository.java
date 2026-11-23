package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.XpSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataXpTransactionRepository extends JpaRepository<XpTransactionJpaEntity, UUID> {
    boolean existsByUserIdAndSourceTypeAndSourceId(UUID userId, XpSourceType sourceType, String sourceId);
}
