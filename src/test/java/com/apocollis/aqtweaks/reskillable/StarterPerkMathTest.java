package com.apocollis.aqtweaks.reskillable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StarterPerkMathTest {

    // ---- Riposte ----

    @Test
    void parryWindowIncludesTheBoundaryAndRejectsNoShield() {
        assertTrue(StarterPerkMath.isParry(0, 10));
        assertTrue(StarterPerkMath.isParry(10, 10));
        assertFalse(StarterPerkMath.isParry(11, 10));
        assertFalse(StarterPerkMath.isParry(-1, 10));
    }

    // ---- Steady Draw ----

    @Test
    void withoutThePerkTheHoldIsBilledEveryInterval() {
        assertFalse(StarterPerkMath.billsBowHold(false, 0, 10, 20));
        assertTrue(StarterPerkMath.billsBowHold(false, 10, 10, 20));
        assertFalse(StarterPerkMath.billsBowHold(false, 11, 10, 20));
        assertTrue(StarterPerkMath.billsBowHold(false, 20, 10, 20));
    }

    @Test
    void steadyDrawBillsOnlyAfterFullDrawAtTheNormalInterval() {
        for (int t = 1; t <= 20; t++) {
            assertFalse(StarterPerkMath.billsBowHold(true, t, 10, 20), "tick " + t);
        }
        assertFalse(StarterPerkMath.billsBowHold(true, 25, 10, 20));
        assertTrue(StarterPerkMath.billsBowHold(true, 30, 10, 20));
        assertTrue(StarterPerkMath.billsBowHold(true, 40, 10, 20));
        assertFalse(StarterPerkMath.billsBowHold(true, 35, 10, 20));
    }

    @Test
    void badIntervalNeverBills() {
        assertFalse(StarterPerkMath.billsBowHold(true, 50, 0, 20));
        assertFalse(StarterPerkMath.billsBowHold(false, 50, -1, 20));
    }

    @Test
    void arrowReturnNeedsPickupVanillaEntityAndALowRoll() {
        assertTrue(StarterPerkMath.returnsArrow(true, true, 0.74D, 0.75D));
        assertFalse(StarterPerkMath.returnsArrow(true, true, 0.75D, 0.75D));
        assertFalse(StarterPerkMath.returnsArrow(false, true, 0.0D, 1.0D));
        assertFalse(StarterPerkMath.returnsArrow(true, false, 0.0D, 1.0D));
        assertFalse(StarterPerkMath.returnsArrow(true, true, 0.0D, 0.0D));
    }

    // ---- Fleetfoot ----

    @Test
    void freeSprintWindowIsTheFirstTicksOfTheRun() {
        assertTrue(StarterPerkMath.freeSprintTick(1, 60));
        assertTrue(StarterPerkMath.freeSprintTick(59, 60));
        assertFalse(StarterPerkMath.freeSprintTick(60, 60));
        assertFalse(StarterPerkMath.freeSprintTick(1, 0));
    }

    @Test
    void freeStartOnlyRearmsAfterIdleTime() {
        assertTrue(StarterPerkMath.sprintFreeStartArmed(40, 40));
        assertFalse(StarterPerkMath.sprintFreeStartArmed(10, 40));
        assertTrue(StarterPerkMath.sprintFreeStartArmed(0, 0));
    }

    @Test
    void sneakModifierGivesBackTheRequestedShareOfThePenalty() {
        // restore 0.5: sneak speed 0.3 -> 0.65 of walking speed, so the multiplier is 0.65 / 0.3.
        double amount = StarterPerkMath.sneakModifierAmount(0.5D);
        assertEquals(0.65D, 0.3D * (1.0D + amount), 1.0E-9);
        assertEquals(0.0D, StarterPerkMath.sneakModifierAmount(0.0D), 1.0E-9);
        assertEquals(1.0D, 0.3D * (1.0D + StarterPerkMath.sneakModifierAmount(1.0D)), 1.0E-9);
        assertEquals(StarterPerkMath.sneakModifierAmount(1.0D), StarterPerkMath.sneakModifierAmount(5.0D), 1.0E-9);
    }

    // ---- Vein Rhythm ----

    @Test
    void chainBuildsInsideTheWindowAndCaps() {
        int stacks = 0;
        long last = 0L;
        long now = 100L;
        stacks = StarterPerkMath.stacksAfterBreak(stacks, last, now, 40, 5);
        assertEquals(1, stacks);
        last = now;
        for (int i = 0; i < 8; i++) {
            now += 20L;
            stacks = StarterPerkMath.stacksAfterBreak(stacks, last, now, 40, 5);
            last = now;
        }
        assertEquals(5, stacks);
    }

    @Test
    void chainRestartsAfterTheWindow() {
        assertEquals(1, StarterPerkMath.stacksAfterBreak(4, 100L, 141L, 40, 5));
        assertEquals(5, StarterPerkMath.stacksAfterBreak(4, 100L, 140L, 40, 5));
    }

    @Test
    void stacksExpireWhenNoBreakFollows() {
        assertEquals(3, StarterPerkMath.activeStacks(3, 100L, 140L, 40));
        assertEquals(0, StarterPerkMath.activeStacks(3, 100L, 141L, 40));
        assertEquals(0, StarterPerkMath.activeStacks(0, 100L, 101L, 40));
    }

    @Test
    void breakSpeedMultiplierScalesWithStacks() {
        assertEquals(1.0F, StarterPerkMath.breakSpeedMultiplier(0, 0.04D), 1.0E-6F);
        assertEquals(1.2F, StarterPerkMath.breakSpeedMultiplier(5, 0.04D), 1.0E-6F);
        assertEquals(1.0F, StarterPerkMath.breakSpeedMultiplier(-3, 0.04D), 1.0E-6F);
    }
}
