package com.apocollis.aqtweaks.thaumadditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AegisMathTest {

    @Test
    void poolScalesWithLevelAndIsCapped() {
        assertEquals(5.0F, AegisMath.pool(1, 5, 25));
        assertEquals(15.0F, AegisMath.pool(3, 5, 25));
        assertEquals(25.0F, AegisMath.pool(5, 5, 25));
        assertEquals(20.0F, AegisMath.pool(5, 5, 20));
        assertEquals(5.0F, AegisMath.pool(0, 5, 25));
    }

    @Test
    void intervalBillsAfterEachFullInterval() {
        assertFalse(AegisMath.intervalDue(0, 20));
        assertFalse(AegisMath.intervalDue(19, 20));
        assertTrue(AegisMath.intervalDue(20, 20));
        assertTrue(AegisMath.intervalDue(40, 20));
        assertFalse(AegisMath.intervalDue(10, 0));
    }

    @Test
    void frontArcMatchesVanilla() {
        assertTrue(AegisMath.isFrontal(0, 1, 0, 3));
        assertTrue(AegisMath.isFrontal(0, 1, 2, 0.5));
        assertFalse(AegisMath.isFrontal(0, 1, 0, -3));
        assertFalse(AegisMath.isFrontal(0, 1, 3, 0));
        assertFalse(AegisMath.isFrontal(0, 1, 0, 0));
    }

    @Test
    void runeCountRoundsUp() {
        assertEquals(5, AegisMath.runes(25, 5));
        assertEquals(5, AegisMath.runes(21, 5));
        assertEquals(4, AegisMath.runes(20, 5));
        assertEquals(1, AegisMath.runes(0.5F, 5));
        assertEquals(0, AegisMath.runes(0, 5));
    }

    @Test
    void hurtWindowCountsOnlyTheExtra() {
        assertEquals(6.0F, AegisMath.effectiveDamage(6, 4, 30));
        assertEquals(0.0F, AegisMath.effectiveDamage(3, 4, 5));
        assertEquals(2.0F, AegisMath.effectiveDamage(6, 4, 5));
        assertEquals(0.0F, AegisMath.effectiveDamage(0, 4, 50));
    }

    @Test
    void hitAtOrAbovePoolBreaksWithNoOverflow() {
        assertFalse(AegisMath.breaks(10, 4));
        assertEquals(6.0F, AegisMath.remaining(10, 4));
        assertTrue(AegisMath.breaks(10, 10));
        assertTrue(AegisMath.breaks(10, 25));
        assertEquals(0.0F, AegisMath.remaining(10, 25));
        assertFalse(AegisMath.breaks(10, 0));
    }

    @Test
    void complexityMatchesIntervalBill() {
        assertEquals(10, AegisMath.complexityFor(2.0D));
        assertEquals(1, AegisMath.complexityFor(0.0D));
        assertEquals(5, AegisMath.complexityFor(1.0D));
    }
}
