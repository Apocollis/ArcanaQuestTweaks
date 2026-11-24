package com.apocollis.aqtweaks.additions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InfinityBladeStatsTest {

    @Test
    void defaultDamageAndSpeedMatchDesignedStats() {
        // Default 18 total damage -> +17 over the player's base 1.0
        assertEquals(17.0D, InfinityBladeStats.attackDamageModifier(18.0D), 1e-6);
        // Default 1.6 attacks/sec (vanilla sword speed) -> -2.4 from the player's base 4.0
        assertEquals(-2.4D, InfinityBladeStats.attackSpeedModifier(1.6D), 1e-6);
        // Default durability 6000
        assertEquals(6000, InfinityBladeStats.maxDamage(6000));
    }

    @Test
    void clampsDegenerateConfigValues() {
        assertEquals(0.0D, InfinityBladeStats.attackDamageModifier(0.0D), 1e-6);
        assertEquals(0.0D, InfinityBladeStats.attackDamageModifier(-5.0D), 1e-6);
        assertEquals(-3.9D, InfinityBladeStats.attackSpeedModifier(0.0D), 1e-6);
        assertEquals(1, InfinityBladeStats.maxDamage(0));
        assertEquals(1, InfinityBladeStats.maxDamage(-100));
    }

    @Test
    void normalizesRepairItemId() {
        assertEquals("minecraft:nether_star", InfinityBladeStats.normalizeRepairItemId("  minecraft:nether_star  "));
        assertNull(InfinityBladeStats.normalizeRepairItemId(""));
        assertNull(InfinityBladeStats.normalizeRepairItemId("   "));
        assertNull(InfinityBladeStats.normalizeRepairItemId("none"));
        assertNull(InfinityBladeStats.normalizeRepairItemId("NONE"));
        assertNull(InfinityBladeStats.normalizeRepairItemId(null));
    }
}
