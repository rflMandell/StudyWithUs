package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.CosmeticType;
import com.studywithus.gamification.domain.LevelCosmeticCatalog;
import com.studywithus.gamification.domain.UserCosmeticRepository;
import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquipCosmeticUseCase {

    private final UserLevelRepository userLevelRepository;
    private final UserCosmeticRepository userCosmeticRepository;

    @Transactional
    public UserLevelResult execute(UUID userId, String titleOrCode, String avatarBorderCode) {
        UserLevel userLevel = userLevelRepository.findByUserId(userId)
                .orElseGet(() -> UserLevel.initial(userId));

        if (titleOrCode != null && !titleOrCode.isBlank()) {
            LevelCosmeticCatalog.CosmeticDefinition titleDef = LevelCosmeticCatalog.findTitleByDisplayNameOrCode(titleOrCode)
                    .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "COSMETIC_NOT_FOUND", "Title cosmetic not found"));

            if (userLevel.getCurrentLevel() < titleDef.requiredLevel()) {
                throw new DomainException(HttpStatus.FORBIDDEN, "COSMETIC_LOCKED", "Level too low for this title");
            }
            userLevel.setActiveTitle(titleDef.displayName());
        }

        if (avatarBorderCode != null && !avatarBorderCode.isBlank()) {
            LevelCosmeticCatalog.CosmeticDefinition borderDef = LevelCosmeticCatalog.findByCode(avatarBorderCode)
                    .filter(d -> d.type() == CosmeticType.AVATAR_BORDER)
                    .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "COSMETIC_NOT_FOUND", "Avatar border cosmetic not found"));

            if (userLevel.getCurrentLevel() < borderDef.requiredLevel()) {
                throw new DomainException(HttpStatus.FORBIDDEN, "COSMETIC_LOCKED", "Level too low for this avatar border");
            }
            userLevel.setActiveAvatarBorder(borderDef.code());
        }

        userLevel.setUpdatedAt(Instant.now());
        UserLevel saved = userLevelRepository.save(userLevel);
        return UserLevelResult.from(saved, userCosmeticRepository.findByUserId(userId));
    }
}
