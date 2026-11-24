package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SurgeCycleTest {

    @Test
    void warpMeterGivesElevenTemporaryAndOneNormalOverAnAverageSurge() {
        WarpMeter meter = new WarpMeter();
        int temp = 0;
        int normal = 0;
        for (int i = 0; i < 45; i++) {
            int[] give = meter.exposedCycle(4, 40);
            temp += give[0];
            normal += give[1];
        }
        assertEquals(11, temp);
        assertEquals(1, normal);
    }

    @Test
    void threeCyclesInNeedThreeCyclesOutToAvoidTheNextTemporaryWarp() {
        WarpMeter meter = new WarpMeter();
        for (int i = 0; i < 3; i++) {
            assertEquals(0, meter.exposedCycle(4, 40)[0]);
        }
        for (int i = 0; i < 3; i++) {
            meter.calmCycle();
        }
        assertEquals(0, meter.temp());
        // Back in: the first three cycles again give nothing, the fourth gives the warp.
        for (int i = 0; i < 3; i++) {
            assertEquals(0, meter.exposedCycle(4, 40)[0]);
        }
        assertEquals(1, meter.exposedCycle(4, 40)[0]);
    }

    @Test
    void calmingStopsAtZeroAndTheNormalCounterFollowsTheSamePattern() {
        WarpMeter meter = new WarpMeter();
        for (int i = 0; i < 10; i++) {
            meter.exposedCycle(4, 40);
        }
        assertEquals(10, meter.normal());
        for (int i = 0; i < 25; i++) {
            meter.calmCycle();
        }
        assertEquals(0, meter.normal());
        assertTrue(meter.idle());
        meter.calmCycle();
        assertTrue(meter.idle());
    }

    @Test
    void exposedCycleTriggersExactlyOnTheConfiguredCount() {
        WarpMeter meter = new WarpMeter();
        int normal = 0;
        for (int i = 0; i < 39; i++) normal += meter.exposedCycle(4, 40)[1];
        assertEquals(0, normal);
        assertEquals(1, meter.exposedCycle(4, 40)[1]);
    }

    @Test
    void batteryGainsOneLevelPerFourCyclesAndFillsInAboutForty() {
        double carry = 0.0D;
        int charge = 0;
        int cycles = 0;
        while (charge < 10) {
            LevelAccumulator.Step step = LevelAccumulator.step(carry, 0.25D, 10 - charge);
            carry = step.carry;
            charge += step.levels;
            cycles++;
        }
        assertEquals(40, cycles);
    }

    @Test
    void lowExposureTakesProportionallyLongerAndAFullBatteryDropsItsCarry() {
        double carry = 0.0D;
        int charge = 0;
        int cycles = 0;
        while (charge < 10) {
            LevelAccumulator.Step step = LevelAccumulator.step(carry, 0.125D, 10 - charge);
            carry = step.carry;
            charge += step.levels;
            cycles++;
        }
        assertEquals(80, cycles);
        assertEquals(0.0D, LevelAccumulator.step(0.9D, 0.5D, 0).carry);
        assertEquals(0, LevelAccumulator.step(0.9D, 0.5D, 0).levels);
    }

    @Test
    void updraftRollsRepeatEveryDelay() {
        assertFalse(SurgeExposureRoll.isDue(319L, 16, 0));
        assertTrue(SurgeExposureRoll.isDue(320L, 16, 0));
        assertFalse(SurgeExposureRoll.isDue(639L, 16, 1));
        assertTrue(SurgeExposureRoll.isDue(640L, 16, 1));
    }

    @Test
    void flatChargeScalesWithExposureAndBiomeOnly() {
        assertEquals(5500.0D, SurgeMath.flat(5500.0D, 1.0D, 1.0D));
        assertEquals(2750.0D, SurgeMath.flat(5500.0D, 0.5D, 1.0D));
        assertEquals(50.0D * 1.5D, SurgeMath.flat(50.0D, 1.0D, 1.5D));
        assertEquals(0.0D, SurgeMath.flat(-5.0D, 1.0D, 1.0D));
    }

    @Test
    void anAverageSurgeFillsAboutAQuarterMillionMana() {
        assertEquals(247500.0D, 45 * SurgeMath.flat(5500.0D, 1.0D, 1.0D));
    }
}
