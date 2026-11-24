package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CastBudgetTest {

    @Test
    void startsFullAndSpends() {
        CastBudget b = new CastBudget(100, 20, 0.25, 0.75);
        b.regen(0);
        assertEquals(100.0D, b.energy());
        assertTrue(b.canAfford(25));
        b.spend(25);
        assertEquals(75.0D, b.energy());
        assertFalse(b.holding());
    }

    @Test
    void burstThenHoldThenResume() {
        CastBudget b = new CastBudget(100, 20, 0.25, 0.75);
        b.regen(0);
        b.spend(20);
        b.spend(20);
        b.spend(20);
        assertFalse(b.holding());
        b.spend(20);
        assertTrue(b.holding(), "a pool at 20 percent holds");
        assertFalse(b.canAfford(1), "holding blocks every cast");
        long t = 0;
        while (b.holding() && t < 1000) {
            t++;
            b.regen(t);
        }
        assertTrue(t > 0 && t < 1000);
        assertTrue(b.energy() >= 75.0D);
        assertEquals(75.0D, b.energy(), 1.0D);
        assertTrue(b.canAfford(20));
    }

    @Test
    void holdBackLastsAboutTwoSecondsAtTheDefaults() {
        CastBudget b = new CastBudget(100, 25, 0.25, 0.75);
        b.regen(0);
        b.spend(80);
        assertTrue(b.holding());
        long t = 0;
        while (b.holding()) {
            t++;
            b.regen(t);
        }
        assertTrue(t >= 30 && t <= 60, "held " + t + " ticks");
    }

    @Test
    void heavyCostIsCappedToTheMaximumSoItCanStillBeCast() {
        CastBudget b = new CastBudget(40, 20, 0.25, 0.75);
        b.regen(0);
        assertTrue(b.canAfford(50));
        b.spend(50);
        assertEquals(0.0D, b.energy());
        assertTrue(b.holding());
    }

    @Test
    void doesNotGoAboveMaxOrBelowZero() {
        CastBudget b = new CastBudget(100, 20, 0.25, 0.75);
        b.regen(0);
        b.regen(10_000);
        assertEquals(100.0D, b.energy());
        b.spend(500);
        assertEquals(0.0D, b.energy());
    }
}
