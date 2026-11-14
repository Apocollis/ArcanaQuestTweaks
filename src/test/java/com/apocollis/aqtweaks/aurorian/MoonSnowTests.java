package com.apocollis.aqtweaks.aurorian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class MoonSnowTests {

    // ---- MoonSnowShape ----

    @Test
    void weightsSumToOneAtAnyStrength() {
        for (double s = 0.0; s <= 1.0; s += 0.1) {
            double t = 0;
            for (double w : MoonSnowShape.weights(s)) t += w;
            assertEquals(1.0, t, 1.0E-9);
        }
    }

    @Test
    void classesPickTheirOwnSpritesAndScales() {
        Random r = new Random(5);
        for (MoonSnowShape.SnowClass c : MoonSnowShape.SnowClass.values()) {
            for (int i = 0; i < 500; i++) {
                assertTrue(MoonSnowShape.inSet(c, MoonSnowShape.sprite(c, r)), c.name());
                float sc = MoonSnowShape.scale(c, r);
                assertTrue(sc > 0.0F && sc < 7.0F, c + " " + sc);
                float a = MoonSnowShape.alpha(c, r, 1.0);
                assertTrue(a > 0.0F && a <= 1.0F);
                int life = MoonSnowShape.lifetime(c, r, false);
                assertTrue(life >= 20 && life <= 36);
                int shell = MoonSnowShape.lifetime(c, r, true);
                assertTrue(shell >= 14 && shell <= 20);
            }
        }
    }

    @Test
    void pickFollowsTheWeightsAndStreaksCanBeOff() {
        Random r = new Random(6);
        int puffs = 0;
        int streaks = 0;
        int n = 40000;
        for (int i = 0; i < n; i++) {
            MoonSnowShape.SnowClass c = MoonSnowShape.pick(r, 1.0, false);
            if (c == MoonSnowShape.SnowClass.PUFF) puffs++;
            if (c == MoonSnowShape.SnowClass.STREAK) streaks++;
        }
        assertEquals(0, streaks);
        double w = MoonSnowShape.weights(1.0)[MoonSnowShape.SnowClass.PUFF.ordinal()];
        assertEquals(w, puffs / (double) n, 0.02);
        for (int i = 0; i < 1000; i++) {
            MoonSnowShape.SnowClass c = MoonSnowShape.pickShell(r);
            assertTrue(c != MoonSnowShape.SnowClass.CLUMP && c != MoonSnowShape.SnowClass.STREAK);
        }
    }

    @Test
    void fallIsSpeedTimesTanAndAngleIsClamped() {
        assertEquals(1.5 * Math.tan(Math.toRadians(58.0)), MoonSnowShape.fallPerTick(1.5, 58.0, 0.5), 1.0E-9);
        for (double j = 0; j <= 1.0; j += 0.05) {
            double a = MoonSnowShape.jitteredAngle(80.0, j);
            assertTrue(a >= 4.0 && a <= 82.0, "angle " + a);
            double low = MoonSnowShape.jitteredAngle(5.0, j);
            assertTrue(low >= 4.0);
            assertTrue(MoonSnowShape.fallPerTick(1.0, 80.0, j) > 0.0);
            assertTrue(Double.isFinite(MoonSnowShape.fallPerTick(1.0, 80.0, j)));
        }
    }

    @Test
    void speedAndGustRanges() {
        assertEquals(0.7, MoonSnowShape.horizontalSpeed(0.0, 1.0), 1.0E-9);
        assertEquals(1.6, MoonSnowShape.horizontalSpeed(1.0, 1.0), 1.0E-9);
        for (long t = 0; t < 5000; t += 7) {
            double g = MoonSnowShape.gust(t);
            assertTrue(g >= 0.85 - 1.0E-9 && g <= 1.25 + 1.0E-9);
        }
    }

    @Test
    void perimeterRadiusAndDirection() {
        Random r = new Random(7);
        double R = 10.0;
        double sum = 0;
        int n = 30000;
        for (int i = 0; i < n; i++) {
            double v = MoonSnowShape.perimeterRadius(r, R);
            assertTrue(v >= 7.0 - 1.0E-9 && v <= 18.0 + 1.0E-9, "radius " + v);
            sum += v;
            double[] d = MoonSnowShape.perimeterDirection(r);
            assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1.0E-9);
        }
        // never below 3 even for a tiny radius
        for (int i = 0; i < 2000; i++) assertTrue(MoonSnowShape.perimeterRadius(r, 1.0) >= 3.0 - 1.0E-9);
        // triangular mean is (min + peak + max) / 3 = (7 + 10 + 18) / 3
        assertEquals(35.0 / 3.0, sum / n, 0.15);
    }

    @Test
    void roofKillPredicate() {
        assertFalse(MoonSnowShape.underRoof(70.0, 70));
        assertFalse(MoonSnowShape.underRoof(69.8, 70));
        assertTrue(MoonSnowShape.underRoof(69.0, 70));
        assertFalse(MoonSnowShape.underRoof(10.0, -1));
        assertFalse(MoonSnowShape.underRoof(0.0, 0));
    }

    @Test
    void exposureEaseSettlesInAboutASecond() {
        float e = 0.0F;
        for (int i = 0; i < 20; i++) e = MoonSnowShape.easeExposure(e, 1.0F);
        assertTrue(e > 0.88F && e < 0.95F, "e " + e);
    }

    // ---- SnowBurial ----

    @Test
    void burialLevels() {
        assertEquals(SnowBurial.Level.NONE, SnowBurial.level(65.8, 66.6, 65.0));
        assertEquals(SnowBurial.Level.BODY, SnowBurial.level(65.8, 66.6, 66.0));
        assertEquals(SnowBurial.Level.HEAD, SnowBurial.level(65.8, 66.6, 67.0));
        assertEquals(SnowBurial.Level.BODY, SnowBurial.level(65.8, 66.6, 66.6));
        assertEquals(SnowBurial.Level.NONE, SnowBurial.level(65.8, 66.6, 65.8));
    }

    @Test
    void airDebtAndDamage() {
        int debt = 0;
        for (int i = 0; i < 7; i++) debt = SnowBurial.debtWithHeadUnder(debt, 40);
        assertFalse(SnowBurial.outOfAir(debt));
        debt = SnowBurial.debtWithHeadUnder(debt, 40);
        assertTrue(SnowBurial.outOfAir(debt));
        assertEquals(0, SnowBurial.debtWithHeadUnder(0, 0));
        assertEquals(0, SnowBurial.debtWithHeadFree(100));
        assertEquals(140, SnowBurial.debtWithHeadFree(300));
        assertEquals(2.0F, SnowBurial.damage(20.0F, 2.0F), 1.0E-6F);
        assertEquals(1.0F, SnowBurial.damage(2.0F, 2.0F), 1.0E-6F);
        assertEquals(0.0F, SnowBurial.damage(1.0F, 2.0F), 1.0E-6F);
        assertEquals(0.0F, SnowBurial.damage(0.5F, 2.0F), 1.0E-6F);
        assertEquals(0.0F, SnowBurial.damage(20.0F, 0.0F), 1.0E-6F);
    }
}
