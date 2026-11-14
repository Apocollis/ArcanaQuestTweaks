package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LeyEruptionShapeTest {
    @Test
    void colourMixIsAboutSeventyThirty() {
        Random r = new Random(1);
        int emerald = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            int c = LeyEruptionShape.colour(r);
            if (LeyEruptionShape.isEmerald(c)) emerald++;
        }
        double share = emerald / (double) n;
        assertTrue(share > 0.27 && share < 0.33, "emerald share " + share);
    }

    @Test
    void spritesStayInTheirSets() {
        Random r = new Random(2);
        for (int i = 0; i < 5000; i++) {
            int g = LeyEruptionShape.glyphSprite(r);
            assertTrue(g >= 225 && g <= 250, "glyph " + g);
            int e = LeyEruptionShape.eruptionSprite(r);
            assertTrue((e >= 128 && e <= 135) || (e >= 164 && e <= 167) || (e >= 176 && e <= 180), "eruption " + e);
        }
    }

    @Test
    void lifetimesAndSpeedsStayInRange() {
        Random r = new Random(3);
        for (int i = 0; i < 5000; i++) {
            int gl = LeyEruptionShape.glyphLifetime(r);
            assertTrue(gl >= 36 && gl <= 48);
            int el = LeyEruptionShape.eruptionLifetime(r);
            assertTrue(el >= 22 && el <= 36);
            double w = LeyEruptionShape.glyphRise(r, false);
            assertTrue(w >= 0.16 && w <= 0.28);
            double a = LeyEruptionShape.glyphRise(r, true);
            assertTrue(a >= 0.24 && a <= 0.42);
            double u = LeyEruptionShape.eruptionRise(r);
            assertTrue(u >= 0.30 && u <= 0.72);
        }
    }

    @Test
    void alphaFadesAtBothEnds() {
        assertEquals(0.0F, LeyEruptionShape.alpha(0, 40), 1.0E-6F);
        assertEquals(1.0F, LeyEruptionShape.alpha(20, 40), 1.0E-6F);
        assertEquals(0.0F, LeyEruptionShape.alpha(40, 40), 1.0E-6F);
        assertTrue(LeyEruptionShape.alpha(30, 40) < 1.0F);
        assertEquals(0.0F, LeyEruptionShape.alpha(5, 0), 1.0E-6F);
    }

    @Test
    void offsetsRespectTheKeepOutAndCoverTheSquare() {
        Random r = new Random(4);
        double radius = 48.0;
        boolean corner = false;
        for (int i = 0; i < 20000; i++) {
            double[] o = LeyEruptionShape.offset(r, radius, 8.0);
            assertTrue(o[0] * o[0] + o[1] * o[1] >= 8.0 * 8.0 - 1.0E-9);
            assertTrue(Math.abs(o[0]) <= radius + 1.0E-9 && Math.abs(o[1]) <= radius + 1.0E-9);
            if (Math.abs(o[0]) > 0.9 * radius && Math.abs(o[1]) > 0.9 * radius) corner = true;
        }
        assertTrue(corner, "corners are reached");
    }

    @Test
    void areaAndLinearFactors() {
        assertEquals(9.0, LeyEruptionShape.areaFactor(48.0), 1.0E-9);
        assertEquals(3.0, LeyEruptionShape.linearFactor(48.0), 1.0E-9);
        assertEquals(1.0, LeyEruptionShape.areaFactor(16.0), 1.0E-9);
        assertEquals(75, LeyEruptionShape.attempts(0.6, 14.0, 48.0, 1.0));
        assertEquals(0, LeyEruptionShape.attempts(0.0, 14.0, 48.0, 1.0));
        assertEquals(12, LeyEruptionShape.vents(1.0, 48.0, 1.0));
    }

    @Test
    void distanceScaleIsMonotonicFromOneToOnePointSix() {
        assertEquals(1.0F, LeyEruptionShape.distanceScale(0.0, 48.0), 1.0E-6F);
        assertEquals(1.0F, LeyEruptionShape.distanceScale(8.0, 48.0), 1.0E-6F);
        assertEquals(1.6F, LeyEruptionShape.distanceScale(48.0, 48.0), 1.0E-5F);
        float prev = 0.0F;
        for (double d = 0; d <= 60; d += 0.5) {
            float s = LeyEruptionShape.distanceScale(d, 48.0);
            assertTrue(s >= prev);
            assertTrue(s <= 1.6F + 1.0E-5F);
            prev = s;
        }
    }

    @Test
    void budgetsNeverExceedShareOrCap() {
        int cap = 1000;
        int vent = 0;
        int ground = 0;
        for (int i = 0; i < 5000; i++) {
            int total = vent + ground;
            if (LeyEruptionShape.ventAllowed(vent, total, cap)) vent++;
            total = vent + ground;
            if (LeyEruptionShape.groundAllowed(vent, total, cap)) ground++;
        }
        assertTrue(vent <= 400, "vent " + vent);
        assertTrue(ground <= 600, "ground " + ground);
        assertTrue(vent + ground <= cap);
        assertFalse(LeyEruptionShape.ventAllowed(0, cap, cap));
        assertFalse(LeyEruptionShape.groundAllowed(0, cap, cap));
    }

    @Test
    void spawnFloorIsTwentyFourBelowThePlayerAndNeverAboveThirty() {
        assertEquals(0, LeyEruptionShape.floorY(10.0D));
        assertEquals(0, LeyEruptionShape.floorY(24.9D));
        assertEquals(1, LeyEruptionShape.floorY(25.0D));
        assertEquals(9, LeyEruptionShape.floorY(33.5D));
        assertEquals(30, LeyEruptionShape.floorY(54.0D));
        assertEquals(30, LeyEruptionShape.floorY(70.0D));
        assertEquals(30, LeyEruptionShape.floorY(250.0D));
    }

    @Test
    void peaksAreFiftyToFiftyFiveAndRisesStaySteady() {
        Random r = new Random(5);
        for (int i = 0; i < 5000; i++) {
            double peak = LeyEruptionShape.peakY(r);
            assertTrue(peak >= 50.0D && peak < 55.0D);
            double rune = LeyEruptionShape.riseSpeed(r, true);
            assertTrue(rune >= 0.42D && rune <= 0.62D);
            double shape = LeyEruptionShape.riseSpeed(r, false);
            assertTrue(shape >= 0.50D && shape <= 0.78D);
        }
    }

    @Test
    void lifetimeReachesThePeakFromEveryFloor() {
        // From Y0 at the slowest rune speed, and from Y30 at the fastest other shape.
        assertEquals(131, LeyEruptionShape.maxAge(0.0D, 55.0D, 0.42D));
        assertEquals(26, LeyEruptionShape.maxAge(30.0D, 50.0D, 0.78D));
        assertEquals(1, LeyEruptionShape.maxAge(60.0D, 55.0D, 0.5D));
        for (int floor = 0; floor <= 30; floor += 5) {
            int age = LeyEruptionShape.maxAge(floor, 52.0D, 0.6D);
            assertTrue(floor + age * 0.6D >= 52.0D);
        }
    }

    @Test
    void taperIsFullUpToFiftyAndGoneAtThePeak() {
        assertEquals(1.0F, LeyEruptionShape.taper(10.0D, 53.0D), 1.0E-6F);
        assertEquals(1.0F, LeyEruptionShape.taper(50.0D, 53.0D), 1.0E-6F);
        assertEquals(0.5F, LeyEruptionShape.taper(51.5D, 53.0D), 1.0E-6F);
        assertEquals(0.0F, LeyEruptionShape.taper(53.0D, 53.0D), 1.0E-6F);
        assertEquals(0.0F, LeyEruptionShape.taper(60.0D, 53.0D), 1.0E-6F);
        // Different peaks dissolve at different heights.
        assertTrue(LeyEruptionShape.taper(52.0D, 55.0D) > LeyEruptionShape.taper(52.0D, 52.5D));
        assertEquals(0.0F, LeyEruptionShape.alpha(0, 60, 20.0D, 53.0D), 1.0E-6F);
        assertEquals(1.0F, LeyEruptionShape.alpha(30, 60, 20.0D, 53.0D), 1.0E-6F);
        assertEquals(0.0F, LeyEruptionShape.alpha(30, 60, 54.0D, 53.0D), 1.0E-6F);
    }

    @Test
    void aboutThirtyPercentAreRunes() {
        Random r = new Random(6);
        int runes = 0;
        int n = 30000;
        for (int i = 0; i < n; i++) {
            if (LeyEruptionShape.isRune(r)) runes++;
        }
        assertEquals(0.30D, runes / (double) n, 0.02D);
    }

    @Test
    void spreadPutsAboutHalfTheSpawnsNearThePlayer() {
        Random r = new Random(7);
        int near = 0;
        int n = 30000;
        for (int i = 0; i < n; i++) {
            double[] o = LeyEruptionShape.spreadOffset(r, 48.0D, LeyEruptionShape.WARNING_KEEP_OUT);
            double d = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
            assertTrue(d >= LeyEruptionShape.WARNING_KEEP_OUT - 1.0E-9D);
            assertTrue(Math.abs(o[0]) <= 48.0D && Math.abs(o[1]) <= 48.0D);
            if (d <= LeyEruptionShape.NEAR_MAX + 1.0E-9D) near++;
        }
        assertEquals(LeyEruptionShape.NEAR_SHARE, near / (double) n, 0.02D);
    }

    @Test
    void smallRadiusStaysInsideIt() {
        Random r = new Random(8);
        for (int i = 0; i < 2000; i++) {
            double[] o = LeyEruptionShape.spreadOffset(r, 12.0D, 3.0D);
            double d = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
            assertTrue(d >= 3.0D - 1.0E-9D && d <= 12.0D + 1.0E-9D);
        }
    }
}
