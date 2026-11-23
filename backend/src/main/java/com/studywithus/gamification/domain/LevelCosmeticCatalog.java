package com.studywithus.gamification.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class LevelCosmeticCatalog {

    public record CosmeticDefinition(
            String code,
            String displayName,
            CosmeticType type,
            int requiredLevel
    ) {}

    private static final List<CosmeticDefinition> DEFINITIONS = List.of(
            new CosmeticDefinition("TITLE_CALOURO", "Calouro", CosmeticType.TITLE, 1),
            new CosmeticDefinition("BORDER_NONE", "Sem Borda", CosmeticType.AVATAR_BORDER, 1),
            new CosmeticDefinition("TITLE_ESTUDANTE_DEDICADO", "Estudante Dedicado", CosmeticType.TITLE, 2),
            new CosmeticDefinition("BORDER_BRONZE", "Borda Bronze", CosmeticType.AVATAR_BORDER, 2),
            new CosmeticDefinition("TITLE_EXPLORADOR_SABER", "Explorador do Saber", CosmeticType.TITLE, 3),
            new CosmeticDefinition("BORDER_SILVER", "Borda Prata", CosmeticType.AVATAR_BORDER, 3),
            new CosmeticDefinition("TITLE_ESTRATEGISTA", "Estrategista Acadêmico", CosmeticType.TITLE, 4),
            new CosmeticDefinition("BORDER_GOLD", "Borda Ouro", CosmeticType.AVATAR_BORDER, 4),
            new CosmeticDefinition("TITLE_MESTRE_CONSISTENCIA", "Mestre da Consistência", CosmeticType.TITLE, 5),
            new CosmeticDefinition("BORDER_EMERALD", "Borda Esmeralda", CosmeticType.AVATAR_BORDER, 5),
            new CosmeticDefinition("TITLE_ERUDITO", "Erudito", CosmeticType.TITLE, 7),
            new CosmeticDefinition("BORDER_DIAMOND", "Borda Diamante", CosmeticType.AVATAR_BORDER, 7),
            new CosmeticDefinition("TITLE_LENDA_ESTUDOS", "Lenda dos Estudos", CosmeticType.TITLE, 10),
            new CosmeticDefinition("BORDER_AURORA", "Borda Aurora", CosmeticType.AVATAR_BORDER, 10)
    );

    private LevelCosmeticCatalog() {}

    public static List<CosmeticDefinition> getUnlockedUpToLevel(int level) {
        return DEFINITIONS.stream()
                .filter(d -> d.requiredLevel() <= level)
                .toList();
    }

    public static String getDefaultTitleForLevel(int level) {
        return DEFINITIONS.stream()
                .filter(d -> d.type() == CosmeticType.TITLE && d.requiredLevel() <= level)
                .max(Comparator.comparingInt(CosmeticDefinition::requiredLevel))
                .map(CosmeticDefinition::displayName)
                .orElse("Calouro");
    }

    public static String getDefaultBorderForLevel(int level) {
        return DEFINITIONS.stream()
                .filter(d -> d.type() == CosmeticType.AVATAR_BORDER && d.requiredLevel() <= level)
                .max(Comparator.comparingInt(CosmeticDefinition::requiredLevel))
                .map(CosmeticDefinition::code)
                .orElse("BORDER_NONE");
    }

    public static Optional<CosmeticDefinition> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return DEFINITIONS.stream()
                .filter(d -> d.code().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    public static Optional<CosmeticDefinition> findTitleByDisplayNameOrCode(String titleOrCode) {
        if (titleOrCode == null || titleOrCode.isBlank()) {
            return Optional.empty();
        }
        String trimmed = titleOrCode.trim();
        return DEFINITIONS.stream()
                .filter(d -> d.type() == CosmeticType.TITLE
                        && (d.code().equalsIgnoreCase(trimmed) || d.displayName().equalsIgnoreCase(trimmed)))
                .findFirst();
    }
}
