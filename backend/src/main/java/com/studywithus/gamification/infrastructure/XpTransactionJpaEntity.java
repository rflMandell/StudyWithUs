package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.XpSourceType;
import com.studywithus.gamification.domain.XpTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "xp_transactions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpTransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private XpSourceType sourceType;

    @Column(name = "source_id", nullable = false, length = 120)
    private String sourceId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static XpTransactionJpaEntity fromDomain(XpTransaction domain) {
        return XpTransactionJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .sourceType(domain.getSourceType())
                .sourceId(domain.getSourceId())
                .amount(domain.getAmount())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public XpTransaction toDomain() {
        return XpTransaction.builder()
                .id(id)
                .userId(userId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .amount(amount)
                .createdAt(createdAt)
                .build();
    }
}
