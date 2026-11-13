package com.apocollis.aqtweaks.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AetherSquallGearTest {
    @Test
    void pushFactorOrdering() {
        assertEquals(1.0D, AetherSquallGear.pushFactor(false, false));
        assertTrue(AetherSquallGear.pushFactor(false, true) < 1.0D);
        assertTrue(AetherSquallGear.pushFactor(true, false) < AetherSquallGear.pushFactor(false, true));
    }

    @Test
    void conductivityAttractsButInsulationAvoidsLockOn() {
        assertTrue(AetherSquallGear.attractionWeight(false, true) > AetherSquallGear.attractionWeight(false, false));
        assertTrue(AetherSquallGear.locksOn(false, true));
        assertFalse(AetherSquallGear.locksOn(true, true));
        assertFalse(AetherSquallGear.locksOn(false, false));
    }

    @Test
    void insulatedTakesReducedShock() {
        assertEquals(2.0D, AetherSquallGear.shockDamage(true, 6.0D, 2.0D));
        assertEquals(6.0D, AetherSquallGear.shockDamage(false, 6.0D, 2.0D));
    }
}
