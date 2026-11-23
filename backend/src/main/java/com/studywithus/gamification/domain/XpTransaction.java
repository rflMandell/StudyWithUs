package com.studywithus.gamification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpTransaction {
    private UUID id;
    private UUID userId;
    private XpSourceType sourceType;
    private String sourceId;
    private long amount;
    private Instant createdAt;

    public static XpTransaction create(UUID userId, XpSourceType sourceType, String sourceId, long amount) {
        return XpTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .amount(amount)
                .createdAt(Instant.now())
                .build();
    }
}
