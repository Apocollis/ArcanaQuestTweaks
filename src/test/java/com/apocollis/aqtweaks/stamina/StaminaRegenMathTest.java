package com.apocollis.aqtweaks.stamina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.apocollis.aqtweaks.stamina.StaminaRegenMath.Pace;
import org.junit.jupiter.api.Test;

class StaminaRegenMathTest {

    private static final int NC = StaminaRegenMath.NO_CAP;

    private static int iv(Pace p, boolean cardio, boolean sprint, int vigor, int repl) {
        return StaminaRegenMath.interval(p, cardio, sprint, 10, 20, 30, vigor, repl);
    }

    @Test
    void baseStates() {
        assertEquals(10, iv(Pace.RESTING, false, false, 30, NC));
        assertEquals(20, iv(Pace.MOVING, false, false, 30, NC));
        assertEquals(30, iv(Pace.ACTIVE, false, false, 30, NC));
    }

    @Test
    void restedVigorCapsEveryState() {
        assertEquals(10, iv(Pace.RESTING, false, false, 15, NC));
        assertEquals(15, iv(Pace.MOVING, false, false, 15, NC));
        assertEquals(15, iv(Pace.ACTIVE, false, false, 15, NC));
        assertEquals(25, iv(Pace.ACTIVE, false, false, 25, NC));
        assertEquals(20, iv(Pace.ACTIVE, false, false, 20, NC));
        assertEquals(20, iv(Pace.MOVING, false, false, 25, NC));
    }

    @Test
    void replenishmentOnlyLowersActive() {
        int r0 = StaminaRegenMath.replenishment(0, 15, 5, 5);
        assertEquals(15, iv(Pace.ACTIVE, false, false, 30, r0));
        assertEquals(20, iv(Pace.MOVING, false, false, 30, r0));
        assertEquals(10, iv(Pace.RESTING, false, false, 30, r0));
    }

    @Test
    void replenishmentAmplifiersScaleAndFloor() {
        assertEquals(15, StaminaRegenMath.replenishment(0, 15, 5, 5));
        assertEquals(10, StaminaRegenMath.replenishment(1, 15, 5, 5));
        assertEquals(5, StaminaRegenMath.replenishment(2, 15, 5, 5));
        assertEquals(5, StaminaRegenMath.replenishment(3, 15, 5, 5));
        assertEquals(5, StaminaRegenMath.replenishment(9, 15, 5, 5));
        assertEquals(NC, StaminaRegenMath.replenishment(-1, 15, 5, 5));
        assertEquals(NC, StaminaRegenMath.replenishment(0, 0, 5, 5));
    }

    @Test
    void restedAndReplenishmentDoNotStack() {
        assertEquals(15, iv(Pace.ACTIVE, false, false, 15, 15));
        assertEquals(10, iv(Pace.ACTIVE, false, false, 15, 10));
    }

    @Test
    void cardioMasterMakesActiveMoving() {
        assertEquals(20, iv(Pace.ACTIVE, true, false, 30, NC));
        assertEquals(20, iv(Pace.MOVING, true, false, 30, NC));
        assertEquals(10, iv(Pace.RESTING, true, false, 30, NC));
        // Rested III and Replenishment still win; Rested II ties.
        assertEquals(15, iv(Pace.ACTIVE, true, false, 15, NC));
        assertEquals(20, iv(Pace.ACTIVE, true, false, 20, NC));
        assertEquals(10, iv(Pace.ACTIVE, true, false, 30, 10));
    }

    @Test
    void marathonSprintIsMovingButNeverWorseThanActive() {
        assertEquals(20, iv(Pace.MOVING, false, true, 30, NC));
        // Replenishment 15 would give Active 15: the sprint must not be slower than that.
        assertEquals(15, iv(Pace.MOVING, false, true, 30, 15));
        assertEquals(5, iv(Pace.MOVING, false, true, 30, 5));
        // Plain moving (not sprint-like) ignores Replenishment.
        assertEquals(20, iv(Pace.MOVING, false, false, 30, 15));
    }

    @Test
    void netSprintStaysZeroWithMarathonRunner() {
        // Sprint costs 1 per 20 ticks; Moving refills 1 per 20 ticks.
        assertEquals(20, iv(Pace.MOVING, true, true, 30, NC));
    }

    @Test
    void neverBelowOneTick() {
        assertEquals(1, StaminaRegenMath.interval(Pace.RESTING, false, false, 0, 0, 0, 0, 0));
    }

    @Test
    void swingWindowIsBaseGracePlusAttackPeriod() {
        assertEquals(28, StaminaRegenMath.swingWindow(20, 8));
        assertEquals(34, StaminaRegenMath.swingWindow(20, 14));
        assertEquals(40, StaminaRegenMath.swingWindow(20, 20));
        assertEquals(21, StaminaRegenMath.swingWindow(20, 0));
    }
}
