package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.XpSourceType;
import com.studywithus.gamification.domain.XpTransaction;
import com.studywithus.gamification.domain.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class XpTransactionRepositoryAdapter implements XpTransactionRepository {

    private final SpringDataXpTransactionRepository repository;

    @Override
    public XpTransaction save(XpTransaction transaction) {
        return repository.save(XpTransactionJpaEntity.fromDomain(transaction)).toDomain();
    }

    @Override
    public boolean existsByUserIdAndSourceTypeAndSourceId(UUID userId, XpSourceType sourceType, String sourceId) {
        return repository.existsByUserIdAndSourceTypeAndSourceId(userId, sourceType, sourceId);
    }
}
