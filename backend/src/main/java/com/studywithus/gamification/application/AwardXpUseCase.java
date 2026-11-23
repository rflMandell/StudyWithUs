package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.LevelCosmeticCatalog;
import com.studywithus.gamification.domain.UserCosmetic;
import com.studywithus.gamification.domain.UserCosmeticRepository;
import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import com.studywithus.gamification.domain.UserLevelUpEvent;
import com.studywithus.gamification.domain.XpTransaction;
import com.studywithus.gamification.domain.XpTransactionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class AwardXpUseCase {

    private final UserLevelRepository userLevelRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final UserCosmeticRepository userCosmeticRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter xpAwardedCounter;
    private final Counter levelUpsCounter;

    @Autowired
    public AwardXpUseCase(
            UserLevelRepository userLevelRepository,
            XpTransactionRepository xpTransactionRepository,
            UserCosmeticRepository userCosmeticRepository,
            ApplicationEventPublisher eventPublisher,
            @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        this.userLevelRepository = userLevelRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.userCosmeticRepository = userCosmeticRepository;
        this.eventPublisher = eventPublisher;
        this.xpAwardedCounter = meterRegistry != null
                ? Counter.builder("studywithus_xp_awarded_total")
                        .description("Total XP awarded to users")
                        .register(meterRegistry)
                : null;
        this.levelUpsCounter = meterRegistry != null
                ? Counter.builder("studywithus_level_ups_total")
                        .description("Total number of user level-ups")
                        .register(meterRegistry)
                : null;
    }

    @Transactional
    public UserLevelResult execute(AwardXpCommand command) {
        if (command.userId() == null || command.sourceType() == null || command.sourceId() == null || command.amount() <= 0) {
            throw new IllegalArgumentException("Invalid XP award command");
        }

        UserLevel userLevel = userLevelRepository.findByUserId(command.userId())
                .orElseGet(() -> userLevelRepository.save(UserLevel.initial(command.userId())));

        // Idempotency guard: never award XP twice for the same (userId, sourceType, sourceId)
        if (xpTransactionRepository.existsByUserIdAndSourceTypeAndSourceId(
                command.userId(), command.sourceType(), command.sourceId())) {
            log.debug("Idempotent XP skip for userId={}, sourceType={}, sourceId={}",
                    command.userId(), command.sourceType(), command.sourceId());
            return UserLevelResult.from(userLevel, userCosmeticRepository.findByUserId(command.userId()));
        }

        xpTransactionRepository.save(XpTransaction.create(
                command.userId(),
                command.sourceType(),
                command.sourceId(),
                command.amount()
        ));

        int previousLevel = userLevel.getCurrentLevel();
        boolean leveledUp = userLevel.applyXp(command.amount());
        UserLevel savedLevel = userLevelRepository.save(userLevel);

        if (xpAwardedCounter != null) {
            xpAwardedCounter.increment(command.amount());
        }

        unlockCosmeticsForLevel(command.userId(), savedLevel.getCurrentLevel());

        if (leveledUp) {
            if (levelUpsCounter != null) {
                levelUpsCounter.increment();
            }
            log.info("User {} leveled up: {} -> {} (totalXp={})",
                    command.userId(), previousLevel, savedLevel.getCurrentLevel(), savedLevel.getTotalXp());

            eventPublisher.publishEvent(new UserLevelUpEvent(
                    command.userId(),
                    previousLevel,
                    savedLevel.getCurrentLevel(),
                    savedLevel.getTotalXp(),
                    savedLevel.getActiveTitle(),
                    savedLevel.getActiveAvatarBorder()
            ));
        }

        return UserLevelResult.from(savedLevel, userCosmeticRepository.findByUserId(command.userId()));
    }

    private void unlockCosmeticsForLevel(UUID userId, int level) {
        for (LevelCosmeticCatalog.CosmeticDefinition def : LevelCosmeticCatalog.getUnlockedUpToLevel(level)) {
            if (!userCosmeticRepository.existsByUserIdAndCosmeticCode(userId, def.code())) {
                userCosmeticRepository.save(UserCosmetic.create(userId, def.code(), def.type(), def.requiredLevel()));
            }
        }
    }
}
