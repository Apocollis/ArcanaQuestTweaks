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

    // ---- SnowRate ----

    @Test
    void rateCurveAndCarry() {
        assertEquals(0.0, SnowRate.curve(0.24), 1.0E-9);
        assertEquals(0.25 * 0.25, SnowRate.curve(0.25), 1.0E-9);
        assertEquals(1.0, SnowRate.curve(1.0), 1.0E-9);
        assertTrue(SnowRate.curve(0.8) > SnowRate.curve(0.5));
        SnowRate.Carry carry = new SnowRate.Carry();
        long total = 0;
        for (int i = 0; i < 2000; i++) total += carry.take(7, 8.0, 0.7);
        double expected = 2000 * 7 * 8.0 * SnowRate.curve(0.7) / 20.0;
        assertEquals(expected, total, 1.0);
        assertEquals(0, carry.take(100, 8.0, 0.1));
        assertTrue(SnowRate.canAdd(3, 4));
        assertFalse(SnowRate.canAdd(4, 4));
        assertFalse(SnowRate.canAdd(16, 99));
        assertTrue(SnowRate.canAdd(15, 99));
    }

    // ---- SnowShadow ----

    @Test
    void wallShadesAboutHeightOverTanDownwind() {
        double angle = 58.0;
        double tan = Math.tan(Math.toRadians(angle));
        // wall 10 high standing 1 block upwind of the column: surface 64, wall top height 74
        assertTrue(SnowShadow.inShadow(64, angle, d -> d == 1 ? 74 : 64));
        // the wall is d blocks upwind; it shades while 64 + d * tan < 74
        for (int d : SnowShadow.DISTANCES) {
            boolean shaded = SnowShadow.inShadow(64, angle, dd -> dd == d ? 74 : 64);
            assertEquals(64 + d * tan < 74, shaded, "d=" + d);
        }
        assertFalse(SnowShadow.inShadow(64, angle, d -> 64));
        assertFalse(SnowShadow.inShadow(64, angle, d -> -1));
        // the sample line stops at the build limit
        assertFalse(SnowShadow.inShadow(254, angle, d -> 255));
    }

    // ---- SnowStack ----

    @Test
    void stackArithmetic() {
        assertEquals(0, SnowStack.depth());
        assertEquals(3, SnowStack.depth(3));
        assertEquals(8, SnowStack.depth(8));
        assertEquals(11, SnowStack.depth(8, 3));
        assertEquals(16, SnowStack.depth(8, 8));
        assertEquals(0, SnowStack.addBlockIndex(0));
        assertEquals(0, SnowStack.addBlockIndex(7));
        assertEquals(1, SnowStack.addBlockIndex(8));
        assertEquals(1, SnowStack.addBlockIndex(15));
        assertEquals(1, SnowStack.layersAfterAdd(8));
        assertEquals(8, SnowStack.layersAfterAdd(7));
        assertEquals(8, SnowStack.layersIn(11, 0));
        assertEquals(3, SnowStack.layersIn(11, 1));
        assertEquals(0, SnowStack.layersIn(3, 1));
    }

    // ---- SnowLeveling ----

    private static int[][] grid(int n, int fill) {
        int[][] g = new int[n][n];
        for (int[] row : g) java.util.Arrays.fill(row, fill);
        return g;
    }

    private static int depthAt(int[][] g, long col) {
        int x = SnowLeveling.unpackX(col);
        int z = SnowLeveling.unpackZ(col);
        if (x < 0 || z < 0 || x >= g.length || z >= g.length) return -1;
        return g[x][z];
    }

    @Test
    void layerStaysWhenNoNeighbourIsThatLow() {
        int[][] g = grid(5, 4);
        long start = SnowLeveling.pack(2, 2);
        assertEquals(start, SnowLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
        g[2][3] = 3; // one lower: adding gives a difference of 2, which is allowed
        assertEquals(start, SnowLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
    }

    @Test
    void layerGoesToAShallowerNeighbourAndWalksDownhill() {
        int[][] g = grid(7, 6);
        g[3][4] = 4; // two lower than the start
        long start = SnowLeveling.pack(3, 3);
        assertEquals(SnowLeveling.pack(3, 4), SnowLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
        g[3][5] = 2; // the neighbour of the neighbour is lower still
        assertEquals(SnowLeveling.pack(3, 5), SnowLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
    }

    @Test
    void unconstrainedNeighboursAreIgnoredAndCapsHold() {
        int[][] g = grid(5, 5);
        g[2][3] = -1;
        g[1][2] = -1;
        long start = SnowLeveling.pack(2, 2);
        assertEquals(start, SnowLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
        assertEquals(SnowLeveling.NONE, SnowLeveling.walk(start, c -> depthAt(g, c), 2, 5, 3));
        assertEquals(SnowLeveling.NONE, SnowLeveling.walk(SnowLeveling.pack(9, 9), c -> depthAt(g, c), 2, 16, 3));
        assertEquals(SnowLeveling.NONE, SnowLeveling.walk(SnowLeveling.pack(2, 3), c -> depthAt(g, c), 2, 16, 3));
    }

    @Test
    void randomFillNeverLeavesNeighboursMoreThanTheStepApart() {
        for (int step = 1; step <= 4; step++) {
            int n = 24;
            int[][] g = grid(n, 0);
            Random r = new Random(11 + step);
            for (int i = 0; i < 60000; i++) {
                long start = SnowLeveling.pack(r.nextInt(n), r.nextInt(n));
                long target = SnowLeveling.walk(start, c -> depthAt(g, c), step, 16, SnowLeveling.MAX_HOPS);
                if (target != SnowLeveling.NONE) g[SnowLeveling.unpackX(target)][SnowLeveling.unpackZ(target)]++;
            }
            int worst = 0;
            int max = 0;
            for (int x = 0; x < n; x++) {
                for (int z = 0; z < n; z++) {
                    max = Math.max(max, g[x][z]);
                    if (x + 1 < n) worst = Math.max(worst, Math.abs(g[x][z] - g[x + 1][z]));
                    if (z + 1 < n) worst = Math.max(worst, Math.abs(g[x][z] - g[x][z + 1]));
                }
            }
            assertTrue(max <= 16, "cap " + max);
            assertTrue(worst <= step + 1, "step " + step + " worst " + worst);
        }
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
