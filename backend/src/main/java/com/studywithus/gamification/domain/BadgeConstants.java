package com.studywithus.gamification.domain;

import java.util.UUID;

public final class BadgeConstants {

    public static final UUID BADGE_FIRST_CHECKIN_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    public static final String BADGE_FIRST_CHECKIN_NAME = "Primeiro Check-in";
    public static final String BADGE_FIRST_CHECKIN_DESC = "Realizou o primeiro check-in de estudos na plataforma.";
    public static final String BADGE_FIRST_CHECKIN_IMG = "/badges/first_checkin.png";

    public static final UUID BADGE_STREAK_7_ID = UUID.fromString("b1000000-0000-0000-0000-000000000002");
    public static final String BADGE_STREAK_7_NAME = "7 Dias Seguidos";
    public static final String BADGE_STREAK_7_DESC = "Manteve consistência nos estudos por 7 dias consecutivos.";
    public static final String BADGE_STREAK_7_IMG = "/badges/streak_7.png";

    public static final UUID BADGE_STREAK_30_ID = UUID.fromString("b1000000-0000-0000-0000-000000000003");
    public static final String BADGE_STREAK_30_NAME = "30 Dias Seguidos";
    public static final String BADGE_STREAK_30_DESC = "Manteve consistência nos estudos por 30 dias consecutivos.";
    public static final String BADGE_STREAK_30_IMG = "/badges/streak_30.png";

    public static final UUID BADGE_COMMUNITY_CREATOR_ID = UUID.fromString("b1000000-0000-0000-0000-000000000004");
    public static final String BADGE_COMMUNITY_CREATOR_NAME = "Criador de Comunidade";
    public static final String BADGE_COMMUNITY_CREATOR_DESC = "Fundou uma comunidade de estudos para apoiar outros colegas.";
    public static final String BADGE_COMMUNITY_CREATOR_IMG = "/badges/community_creator.png";

    public static final UUID BADGE_POINTS_100_ID = UUID.fromString("b1000000-0000-0000-0000-000000000005");
    public static final String BADGE_POINTS_100_NAME = "100 Pontos";
    public static final String BADGE_POINTS_100_DESC = "Acumulou 100 pontos de dedicação nos estudos.";
    public static final String BADGE_POINTS_100_IMG = "/badges/points_100.png";

    public static final UUID BADGE_LEAGUE_CHAMPION_ID = UUID.fromString("b1000000-0000-0000-0000-000000000006");
    public static final String BADGE_LEAGUE_CHAMPION_NAME = "Campeão da Semana";
    public static final String BADGE_LEAGUE_CHAMPION_DESC = "Conquistou o 1º lugar no seu grupo semanal da liga.";
    public static final String BADGE_LEAGUE_CHAMPION_IMG = "/badges/league_champion.png";

    public static final UUID BADGE_LEAGUE_GOLD_ID = UUID.fromString("b1000000-0000-0000-0000-000000000007");
    public static final String BADGE_LEAGUE_GOLD_NAME = "Liga Ouro";
    public static final String BADGE_LEAGUE_GOLD_DESC = "Alcançou a divisão Ouro nas ligas competitivas.";
    public static final String BADGE_LEAGUE_GOLD_IMG = "/badges/league_gold.png";

    public static final UUID BADGE_LEAGUE_DIAMOND_ID = UUID.fromString("b1000000-0000-0000-0000-000000000008");
    public static final String BADGE_LEAGUE_DIAMOND_NAME = "Liga Diamante";
    public static final String BADGE_LEAGUE_DIAMOND_DESC = "Alcançou a divisão Diamante nas ligas competitivas.";
    public static final String BADGE_LEAGUE_DIAMOND_IMG = "/badges/league_diamond.png";

    public static final UUID BADGE_LEAGUE_MASTER_ID = UUID.fromString("b1000000-0000-0000-0000-000000000009");
    public static final String BADGE_LEAGUE_MASTER_NAME = "Mestre da Liga";
    public static final String BADGE_LEAGUE_MASTER_DESC = "Alcançou ou manteve-se na divisão máxima Mestre.";
    public static final String BADGE_LEAGUE_MASTER_IMG = "/badges/league_master.png";

    private BadgeConstants() {
    }
}
