package com.apocollis.aqtweaks.thaumadditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShockMathTest {
    @Test
    void damageMatchesFrostFormula() {
        assertEquals(4.0F, ShockMath.baseDamage(3, 1, 1.0F), 1e-6);
        assertEquals(8.0F, ShockMath.baseDamage(3, 5, 1.0F), 1e-6);
        assertEquals(12.0F, ShockMath.baseDamage(3, 5, 1.5F), 1e-6);
    }

    @Test
    void chainFalloffCompounds() {
        assertEquals(8.0F, ShockMath.chainDamage(8.0F, 0.6F, 0), 1e-6);
        assertEquals(4.8F, ShockMath.chainDamage(8.0F, 0.6F, 1), 1e-5);
        assertEquals(2.88F, ShockMath.chainDamage(8.0F, 0.6F, 2), 1e-5);
    }

    @Test
    void wetBonusOnlyWhenWet() {
        assertEquals(6.0F, ShockMath.wetBonus(4.0F, true, 1.5F), 1e-6);
        assertEquals(4.0F, ShockMath.wetBonus(4.0F, false, 1.5F), 1e-6);
    }

    @Test
    void stunScalesWithDuration() {
        assertEquals(10, ShockMath.stunTicks(10, 10, 1));
        assertEquals(30, ShockMath.stunTicks(10, 10, 3));
        assertEquals(50, ShockMath.stunTicks(10, 10, 5));
        assertEquals(10, ShockMath.stunTicks(10, 10, 0));
        assertEquals(0, ShockMath.stunTicks(0, 0, 5));
    }

    @Test
    void shippedStunDefaults() {
        assertEquals(5, ShockMath.stunTicks(5, 5, 1));
        assertEquals(15, ShockMath.stunTicks(5, 5, 3));
        assertEquals(25, ShockMath.stunTicks(5, 5, 5));
    }

    @Test
    void waterConductsOnlyBetweenTwoWetTargets() {
        assertEquals(8.0, ShockMath.jumpRadius(true, true, 4.0, 8.0), 1e-9);
        assertEquals(4.0, ShockMath.jumpRadius(true, false, 4.0, 8.0), 1e-9);
        assertEquals(4.0, ShockMath.jumpRadius(false, true, 4.0, 8.0), 1e-9);
        assertEquals(4.0, ShockMath.jumpRadius(false, false, 4.0, 8.0), 1e-9);
        assertEquals(4.0, ShockMath.jumpRadius(true, true, 4.0, 2.0), 1e-9);
    }

    @Test
    void stunRules() {
        assertTrue(ShockMath.stuns(true, false, false));
        assertTrue(ShockMath.stuns(false, true, true));
        assertFalse(ShockMath.stuns(false, false, true));
        assertFalse(ShockMath.stuns(false, true, false));
    }
}
