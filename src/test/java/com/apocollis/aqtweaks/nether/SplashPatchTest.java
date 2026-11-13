package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SplashPatchTest {
    @Test
    void levelFourHasThirteenCellsWithAGradient() {
        List<SplashPatch.Cell> cells = SplashPatch.cells(4);
        assertEquals(13, cells.size());
        for (SplashPatch.Cell c : cells) {
            int ring = Math.abs(c.dx) + Math.abs(c.dz);
            assertEquals(4 + ring, c.level);
            assertTrue(c.level >= 4 && c.level <= 6);
        }
    }

    @Test
    void levelFiveSkipsTheOuterRing() {
        List<SplashPatch.Cell> cells = SplashPatch.cells(5);
        assertEquals(5, cells.size());
        for (SplashPatch.Cell c : cells) assertTrue(c.level <= 6);
    }

    @Test
    void neverASource() {
        for (int level = 4; level <= 5; level++) {
            for (SplashPatch.Cell c : SplashPatch.cells(level)) assertTrue(c.level > 0);
        }
    }

    @Test
    void safetyAreaCoversThePatchPlusMargin() {
        List<int[]> area = SplashPatch.safetyArea();
        for (SplashPatch.Cell c : SplashPatch.cells(4)) {
            assertTrue(area.stream().anyMatch(a -> a[0] == c.dx && a[1] == c.dz));
        }
        assertTrue(area.stream().anyMatch(a -> a[0] == 3 && a[1] == 0));
    }
}
