package com.apocollis.aqtweaks.nether;

import java.util.ArrayList;
import java.util.List;

/**
 * The thrown-lava gradient of one spout eruption, as offsets from the centre. Centre at {@code centerLevel}
 * (4 or 5), the four neighbours one level higher, the next diamond ring two higher; levels above 6 are left to
 * vanilla, which adds a level 7 ring. Every cell is flowing lava, never a source. No Minecraft types.
 */
public final class SplashPatch {
    private SplashPatch() {}

    public static final class Cell {
        public final int dx;
        public final int dz;
        public final int level;

        Cell(int dx, int dz, int level) {
            this.dx = dx;
            this.dz = dz;
            this.level = level;
        }
    }

    public static List<Cell> cells(int centerLevel) {
        int c = Math.max(4, Math.min(5, centerLevel));
        List<Cell> out = new ArrayList<>(13);
        out.add(new Cell(0, 0, c));
        int[][] ring1 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : ring1) {
            out.add(new Cell(d[0], d[1], c + 1));
        }
        if (c + 2 <= 6) {
            int[][] ring2 = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] d : ring2) {
                out.add(new Cell(d[0], d[1], c + 2));
            }
        }
        return out;
    }

    /** Offsets of the safety area: the patch plus a one-cell margin (diamond of radius 4 around the centre). */
    public static List<int[]> safetyArea() {
        List<int[]> out = new ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) <= 4) {
                    out.add(new int[] {x, z});
                }
            }
        }
        return out;
    }
}
