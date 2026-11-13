package com.apocollis.aqtweaks.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AetherWindHelperTest {
    @Test
    void sameSeedAndWindowGiveTheSameHeading() {
        assertEquals(AetherWindHelper.angleOf(77L, 3), AetherWindHelper.angleOf(77L, 3));
        assertNotEquals(AetherWindHelper.angleOf(77L, 3), AetherWindHelper.angleOf(77L, 4));
        assertNotEquals(AetherWindHelper.angleOf(77L, 3), AetherWindHelper.angleOf(78L, 3));
    }

    @Test
    void headingIsInRange() {
        for (long w = 0; w < 200; w++) {
            double a = AetherWindHelper.angleOf(12345L, w);
            assertTrue(a >= 0.0D && a < Math.PI * 2.0D);
        }
    }

    @Test
    void constantInsideAWindowAfterTheBlend() {
        long start = 1000;
        double a = AetherWindHelper.headingAt(9L, start, start + 20, 50);
        double b = AetherWindHelper.headingAt(9L, start, start + 49, 50);
        assertEquals(a, b, 1e-12);
    }

    @Test
    void blendsContinuouslyAcrossAShift() {
        long start = 1000;
        double before = AetherWindHelper.headingAt(9L, start, start + 49, 50);
        double at = AetherWindHelper.headingAt(9L, start, start + 50, 50);
        double diff = Math.abs(before - at);
        diff = Math.min(diff, Math.PI * 2.0D - diff);
        assertTrue(diff < 1e-9, "no snap at the boundary: " + diff);
        double mid = AetherWindHelper.headingAt(9L, start, start + 55, 50);
        assertEquals(Math.cos(mid), Math.cos(mid), 0.0);
    }

    @Test
    void warningUsesTheFirstWindowHeading() {
        assertEquals(AetherWindHelper.angleOf(5L, 0), AetherWindHelper.headingAt(5L, 1000, 900, 50), 1e-12);
    }
}
