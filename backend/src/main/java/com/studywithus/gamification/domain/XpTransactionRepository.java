package com.studywithus.gamification.domain;

import java.util.UUID;

public interface XpTransactionRepository {
    XpTransaction save(XpTransaction transaction);
    boolean existsByUserIdAndSourceTypeAndSourceId(UUID userId, XpSourceType sourceType, String sourceId);
}
