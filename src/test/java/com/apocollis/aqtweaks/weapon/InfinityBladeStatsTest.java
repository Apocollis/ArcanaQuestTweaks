package com.apocollis.aqtweaks.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InfinityBladeStatsTest {
    @Test
    void defaultsGiveEighteenDamageAndOnePointSixSpeed() {
        assertEquals(18.0D, InfinityBladeStats.PLAYER_BASE_DAMAGE + InfinityBladeStats.damageModifier(18.0D));
        assertEquals(1.6D, InfinityBladeStats.PLAYER_BASE_SPEED + InfinityBladeStats.speedModifier(1.6D), 1.0e-9);
    }

    @Test
    void damageModifierIsTotalMinusBaseAndNeverNegative() {
        assertEquals(17.0D, InfinityBladeStats.damageModifier(18.0D));
        assertEquals(0.0D, InfinityBladeStats.damageModifier(1.0D));
        assertEquals(0.0D, InfinityBladeStats.damageModifier(0.2D));
        assertEquals(9999.0D, InfinityBladeStats.damageModifier(10000.0D));
    }

    @Test
    void speedModifierIsTotalMinusBase() {
        assertEquals(-2.4D, InfinityBladeStats.speedModifier(1.6D), 1.0e-9);
        assertEquals(0.0D, InfinityBladeStats.speedModifier(4.0D));
        assertEquals(16.0D, InfinityBladeStats.speedModifier(20.0D));
    }

    @Test
    void repairMatchesTheConfiguredItemOnly() {
        assertTrue(InfinityBladeStats.repairMatches("minecraft:nether_star", "minecraft:nether_star"));
        assertTrue(InfinityBladeStats.repairMatches("  minecraft:diamond ", "minecraft:diamond"));
        assertFalse(InfinityBladeStats.repairMatches("minecraft:nether_star", "minecraft:diamond"));
    }

    @Test
    void noneEmptyAndNullDisableRepair() {
        assertFalse(InfinityBladeStats.repairMatches("none", "none"));
        assertFalse(InfinityBladeStats.repairMatches("NONE", "minecraft:nether_star"));
        assertFalse(InfinityBladeStats.repairMatches("", "minecraft:nether_star"));
        assertFalse(InfinityBladeStats.repairMatches(null, "minecraft:nether_star"));
        assertFalse(InfinityBladeStats.repairMatches("minecraft:nether_star", null));
    }
}
