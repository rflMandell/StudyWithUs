package com.studywithus.league.domain;

public enum LeagueDivision {
    BRONZE(1, "Bronze"),
    SILVER(2, "Prata"),
    GOLD(3, "Ouro"),
    DIAMOND(4, "Diamante"),
    MASTER(5, "Mestre");

    private final int tier;
    private final String displayName;

    LeagueDivision(int tier, String displayName) {
        this.tier = tier;
        this.displayName = displayName;
    }

    public int getTier() {
        return tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LeagueDivision promote() {
        return switch (this) {
            case BRONZE -> SILVER;
            case SILVER -> GOLD;
            case GOLD -> DIAMOND;
            case DIAMOND, MASTER -> MASTER;
        };
    }

    public LeagueDivision relegate() {
        return switch (this) {
            case BRONZE, SILVER -> BRONZE;
            case GOLD -> SILVER;
            case DIAMOND -> GOLD;
            case MASTER -> DIAMOND;
        };
    }

    public boolean isHigherThan(LeagueDivision other) {
        return other != null && this.tier > other.tier;
    }
}
