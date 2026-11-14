package com.apocollis.aqtweaks.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LayerTests {

    // ---- LayerRate ----

    @Test
    void rateCurveAndCarry() {
        assertEquals(0.0, LayerRate.curve(0.24), 1.0E-9);
        assertEquals(0.25 * 0.25, LayerRate.curve(0.25), 1.0E-9);
        assertEquals(1.0, LayerRate.curve(1.0), 1.0E-9);
        assertTrue(LayerRate.curve(0.8) > LayerRate.curve(0.5));
        LayerRate.Carry carry = new LayerRate.Carry();
        long total = 0;
        for (int i = 0; i < 2000; i++) total += carry.take(7, 8.0, 0.7);
        double expected = 2000 * 7 * 8.0 * LayerRate.curve(0.7) / 20.0;
        assertEquals(expected, total, 1.0);
        assertEquals(0, carry.take(100, 8.0, 0.1));
        assertTrue(LayerRate.canAdd(3, 4));
        assertFalse(LayerRate.canAdd(4, 4));
        assertFalse(LayerRate.canAdd(16, 99));
        assertTrue(LayerRate.canAdd(15, 99));
    }

    // ---- LayerShadow ----

    @Test
    void wallShadesAboutHeightOverTanDownwind() {
        double angle = 58.0;
        double tan = Math.tan(Math.toRadians(angle));
        // wall 10 high standing 1 block upwind of the column: surface 64, wall top height 74
        assertTrue(LayerShadow.inShadow(64, angle, d -> d == 1 ? 74 : 64));
        // the wall is d blocks upwind; it shades while 64 + d * tan < 74
        for (int d : LayerShadow.DISTANCES) {
            boolean shaded = LayerShadow.inShadow(64, angle, dd -> dd == d ? 74 : 64);
            assertEquals(64 + d * tan < 74, shaded, "d=" + d);
        }
        assertFalse(LayerShadow.inShadow(64, angle, d -> 64));
        assertFalse(LayerShadow.inShadow(64, angle, d -> -1));
        // the sample line stops at the build limit
        assertFalse(LayerShadow.inShadow(254, angle, d -> 255));
    }

    // ---- LayerStack ----

    @Test
    void stackArithmetic() {
        assertEquals(0, LayerStack.depth());
        assertEquals(3, LayerStack.depth(3));
        assertEquals(8, LayerStack.depth(8));
        assertEquals(11, LayerStack.depth(8, 3));
        assertEquals(16, LayerStack.depth(8, 8));
        assertEquals(0, LayerStack.addBlockIndex(0));
        assertEquals(0, LayerStack.addBlockIndex(7));
        assertEquals(1, LayerStack.addBlockIndex(8));
        assertEquals(1, LayerStack.addBlockIndex(15));
        assertEquals(1, LayerStack.layersAfterAdd(8));
        assertEquals(8, LayerStack.layersAfterAdd(7));
        assertEquals(8, LayerStack.layersIn(11, 0));
        assertEquals(3, LayerStack.layersIn(11, 1));
        assertEquals(0, LayerStack.layersIn(3, 1));
    }

    // ---- LayerLeveling ----

    private static int[][] grid(int n, int fill) {
        int[][] g = new int[n][n];
        for (int[] row : g) java.util.Arrays.fill(row, fill);
        return g;
    }

    private static int depthAt(int[][] g, long col) {
        int x = LayerLeveling.unpackX(col);
        int z = LayerLeveling.unpackZ(col);
        if (x < 0 || z < 0 || x >= g.length || z >= g.length) return -1;
        return g[x][z];
    }

    @Test
    void layerStaysWhenNoNeighbourIsThatLow() {
        int[][] g = grid(5, 4);
        long start = LayerLeveling.pack(2, 2);
        assertEquals(start, LayerLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
        g[2][3] = 3; // one lower: adding gives a difference of 2, which is allowed
        assertEquals(start, LayerLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
    }

    @Test
    void layerGoesToAShallowerNeighbourAndWalksDownhill() {
        int[][] g = grid(7, 6);
        g[3][4] = 4; // two lower than the start
        long start = LayerLeveling.pack(3, 3);
        assertEquals(LayerLeveling.pack(3, 4), LayerLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
        g[3][5] = 2; // the neighbour of the neighbour is lower still
        assertEquals(LayerLeveling.pack(3, 5), LayerLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
    }

    @Test
    void unconstrainedNeighboursAreIgnoredAndCapsHold() {
        int[][] g = grid(5, 5);
        g[2][3] = -1;
        g[1][2] = -1;
        long start = LayerLeveling.pack(2, 2);
        assertEquals(start, LayerLeveling.walk(start, c -> depthAt(g, c), 2, 16, 3));
        assertEquals(LayerLeveling.NONE, LayerLeveling.walk(start, c -> depthAt(g, c), 2, 5, 3));
        assertEquals(LayerLeveling.NONE, LayerLeveling.walk(LayerLeveling.pack(9, 9), c -> depthAt(g, c), 2, 16, 3));
        assertEquals(LayerLeveling.NONE, LayerLeveling.walk(LayerLeveling.pack(2, 3), c -> depthAt(g, c), 2, 16, 3));
    }

    @Test
    void randomFillNeverLeavesNeighboursMoreThanTheStepApart() {
        for (int step = 1; step <= 4; step++) {
            int n = 24;
            int[][] g = grid(n, 0);
            Random r = new Random(11 + step);
            for (int i = 0; i < 60000; i++) {
                long start = LayerLeveling.pack(r.nextInt(n), r.nextInt(n));
                long target = LayerLeveling.walk(start, c -> depthAt(g, c), step, 16, LayerLeveling.MAX_HOPS);
                if (target != LayerLeveling.NONE) g[LayerLeveling.unpackX(target)][LayerLeveling.unpackZ(target)]++;
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


    // ---- the sand settings: cap 6 and a 35 degree wind shadow ----

    @Test
    void sandDefaultsCapAtSixAndShadowAtThirtyFive() {
        assertTrue(LayerRate.canAdd(5, 6));
        assertFalse(LayerRate.canAdd(6, 6));
        // a 10 high wall shades about 10 / tan(35) = 14 blocks downwind, but the samples stop at 10
        assertTrue(LayerShadow.inShadow(64, 35.0, d -> d == 10 ? 74 : 64) == (64 + 10 * Math.tan(Math.toRadians(35.0)) < 74));
        assertTrue(LayerShadow.inShadow(64, 35.0, d -> d == 1 ? 70 : 64));
        assertFalse(LayerShadow.inShadow(64, 35.0, d -> 64));
        int[][] g = new int[24][24];
        Random r = new Random(21);
        for (int i = 0; i < 40000; i++) {
            long start = LayerLeveling.pack(r.nextInt(24), r.nextInt(24));
            long t = LayerLeveling.walk(start, c -> {
                int x = LayerLeveling.unpackX(c);
                int z = LayerLeveling.unpackZ(c);
                return x < 0 || z < 0 || x >= 24 || z >= 24 ? -1 : g[x][z];
            }, 2, 6, LayerLeveling.MAX_HOPS);
            if (t != LayerLeveling.NONE) g[LayerLeveling.unpackX(t)][LayerLeveling.unpackZ(t)]++;
        }
        for (int[] row : g) for (int v : row) assertTrue(v <= 6);
    }
}
