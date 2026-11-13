package com.apocollis.aqtweaks.aurorian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RimeTrackerTest {

    @Test
    void standingStillBuildsByTierGain() {
        double rime = 0.0D;
        for (int i = 0; i < 4; i++) {
            rime = RimeTracker.step(rime, false, false, false, 1.0D);
        }
        assertEquals(4.0D, rime, 1.0E-9D);
        assertEquals(4, RimeTracker.stack(rime));
        assertEquals(1, RimeTracker.stack(RimeTracker.step(0.0D, false, false, false, 0.25D) * 4.0D));
    }

    @Test
    void capsAtFifteen() {
        double rime = 14.5D;
        rime = RimeTracker.step(rime, false, false, false, 1.0D);
        assertEquals(15.0D, rime, 1.0E-9D);
    }

    @Test
    void movingShelterAndWarmthThaw() {
        assertEquals(8.0D, RimeTracker.step(10.0D, true, false, false, 1.0D), 1.0E-9D);
        assertEquals(8.0D, RimeTracker.step(10.0D, false, true, false, 1.0D), 1.0E-9D);
        assertEquals(7.0D, RimeTracker.step(10.0D, false, true, true, 1.0D), 1.0E-9D);
        assertEquals(0.0D, RimeTracker.step(1.0D, true, false, false, 1.0D), 1.0E-9D);
    }

    @Test
    void stillThresholdScalesWithSpeedSoSlownessCanStillThaw() {
        double walk = 0.1D;
        double slowV = 0.1D * 0.4D; // Slowness V is -75%, use a gentler -60% here
        double fullThreshold = RimeTracker.stillThreshold(walk, 0.5D);
        double slowThreshold = RimeTracker.stillThreshold(slowV, 0.5D);
        assertTrue(slowThreshold < fullThreshold);
        // Walking at the slowed speed (speed * 43 blocks/s) always clears the slowed threshold.
        assertTrue(RimeTracker.isMoving(slowV * RimeTracker.BLOCKS_PER_SECOND_PER_SPEED, slowV, 0.5D));
        // Sneak-walking at about 30% of normal speed is below the normal threshold.
        assertFalse(RimeTracker.isMoving(0.3D * walk * RimeTracker.BLOCKS_PER_SECOND_PER_SPEED, walk, 0.5D));
        assertFalse(RimeTracker.isMoving(0.0D, walk, 0.5D));
    }

    @Test
    void stackThresholds() {
        assertEquals(-1, RimeTracker.slownessAmplifier(4));
        assertEquals(0, RimeTracker.slownessAmplifier(5));
        assertEquals(2, RimeTracker.slownessAmplifier(10));
        assertEquals(4, RimeTracker.slownessAmplifier(15));
        assertEquals(-1, RimeTracker.fatigueAmplifier(9));
        assertEquals(0, RimeTracker.fatigueAmplifier(10));
        assertEquals(2, RimeTracker.fatigueAmplifier(15));
        assertFalse(RimeTracker.doublesDamage(14));
        assertTrue(RimeTracker.doublesDamage(15));
    }
}
