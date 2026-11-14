package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.twilightforest.SurgeExposureRoll.Outcome;

import java.util.Random;

import org.junit.jupiter.api.Test;

class SurgeExposureRollTest {

    @Test
    void exposureThresholdAtSixteenSeconds() {
        assertFalse(SurgeExposureRoll.isDue(0L, 16));
        assertFalse(SurgeExposureRoll.isDue(319L, 16));
        assertTrue(SurgeExposureRoll.isDue(320L, 16));
        assertTrue(SurgeExposureRoll.isDue(400L, 16));
        assertFalse(SurgeExposureRoll.isDue(400L, 0));
    }

    @Test
    void weightedDistributionMatchesOneTwoTwo() {
        Random rand = new Random(12345L);
        int none = 0;
        int flight = 0;
        int levitation = 0;
        int samples = 10000;
        for (int i = 0; i < samples; i++) {
            Outcome out = SurgeExposureRoll.roll(1, 2, 2, rand);
            if (out == Outcome.NONE) none++;
            else if (out == Outcome.FLIGHT) flight++;
            else levitation++;
        }
        assertEquals(0.20D, none / (double) samples, 0.025D);
        assertEquals(0.40D, flight / (double) samples, 0.025D);
        assertEquals(0.40D, levitation / (double) samples, 0.025D);

        // Zero total weights always returns NONE cleanly
        assertEquals(Outcome.NONE, SurgeExposureRoll.roll(0, 0, 0, rand));
    }

    @Test
    void durationsAndSlowfallBufferMath() {
        assertEquals(320, SurgeExposureRoll.durationTicks(16));
        assertEquals(520, SurgeExposureRoll.slowFallTicks(16, 10));
        assertEquals(320, SurgeExposureRoll.slowFallTicks(16, 0));
    }

    @Test
    void refreshIfShorterLeavesLongerEffectsIntact() {
        assertTrue(SurgeExposureRoll.shouldApply(false, 0, 320));
        assertTrue(SurgeExposureRoll.shouldApply(true, 100, 320));
        assertFalse(SurgeExposureRoll.shouldApply(true, 320, 320));
        assertFalse(SurgeExposureRoll.shouldApply(true, 600, 320));
        assertFalse(SurgeExposureRoll.shouldApply(false, 0, 0));
    }
}
