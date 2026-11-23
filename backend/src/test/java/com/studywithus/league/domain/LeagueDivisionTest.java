package com.studywithus.league.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeagueDivisionTest {

    @Test
    void shouldPromoteAndRelegateAlongHierarchyWithBounds() {
        assertThat(LeagueDivision.BRONZE.promote()).isEqualTo(LeagueDivision.SILVER);
        assertThat(LeagueDivision.SILVER.promote()).isEqualTo(LeagueDivision.GOLD);
        assertThat(LeagueDivision.GOLD.promote()).isEqualTo(LeagueDivision.DIAMOND);
        assertThat(LeagueDivision.DIAMOND.promote()).isEqualTo(LeagueDivision.MASTER);
        assertThat(LeagueDivision.MASTER.promote()).isEqualTo(LeagueDivision.MASTER);

        assertThat(LeagueDivision.MASTER.relegate()).isEqualTo(LeagueDivision.DIAMOND);
        assertThat(LeagueDivision.DIAMOND.relegate()).isEqualTo(LeagueDivision.GOLD);
        assertThat(LeagueDivision.GOLD.relegate()).isEqualTo(LeagueDivision.SILVER);
        assertThat(LeagueDivision.SILVER.relegate()).isEqualTo(LeagueDivision.BRONZE);
        assertThat(LeagueDivision.BRONZE.relegate()).isEqualTo(LeagueDivision.BRONZE);
    }
}
