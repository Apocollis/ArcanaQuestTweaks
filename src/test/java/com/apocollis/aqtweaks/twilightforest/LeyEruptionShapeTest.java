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
}
