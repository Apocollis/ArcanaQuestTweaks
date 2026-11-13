package com.apocollis.aqtweaks.atum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AtumWarningRampTest {

    private static final int WARNING = 600;

    @Test
    void zeroOutsideTheWindow() {
        assertEquals(0.0F, AtumWarningRamp.level(false, 5000, WARNING));
        assertEquals(0.0F, AtumWarningRamp.level(false, WARNING + 1, WARNING));
    }

    @Test
    void zeroWhileStorming() {
        assertEquals(0.0F, AtumWarningRamp.level(true, 300, WARNING));
    }

    @Test
    void zeroForThePinnedCleanWeatherValue() {
        assertEquals(0.0F, AtumWarningRamp.level(false, 1, WARNING));
        assertEquals(0.0F, AtumWarningRamp.level(false, 2, WARNING));
    }

    @Test
    void risesMonotonicallyTowardTheStorm() {
        float last = -1.0F;
        for (int ticks = WARNING; ticks > 2; ticks -= 20) {
            float level = AtumWarningRamp.level(false, ticks, WARNING);
            assertTrue(level >= last, "level fell at " + ticks);
            assertTrue(level >= 0.0F && level <= 1.0F);
            last = level;
        }
        assertTrue(AtumWarningRamp.level(false, 3, WARNING) > 0.99F);
    }

    @Test
    void halfwayIsHalf() {
        assertEquals(0.5F, AtumWarningRamp.level(false, WARNING / 2, WARNING), 0.001F);
    }

    @Test
    void noWarningWindowMeansNoWarning() {
        assertEquals(0.0F, AtumWarningRamp.level(false, 100, 0));
    }
}
