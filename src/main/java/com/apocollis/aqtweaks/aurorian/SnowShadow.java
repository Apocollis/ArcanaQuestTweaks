package com.apocollis.aqtweaks.aurorian;

/**
 * Wind shadow of the falling snow, from the height map only. A column is in shadow when a blocker upwind rises above
 * the slanted flight line to its surface. No Minecraft types.
 */
public final class SnowShadow {
    private SnowShadow() {}

    /** Horizontal distances upwind that are sampled, in blocks. */
    public static final int[] DISTANCES = {1, 2, 3, 4, 6, 8, 10};

    public interface HeightAt {
        /** Precipitation height of the column {@code distance} blocks upwind, or -1 when unknown or unloaded. */
        int at(int distance);
    }

    /**
     * @param surfaceY the height the flakes come down to in this column
     * @param angleDeg flight angle below horizontal (clamped as the particles do)
     */
    public static boolean inShadow(int surfaceY, double angleDeg, HeightAt heights) {
        double tan = Math.tan(Math.toRadians(Math.max(MoonSnowShape.MIN_ANGLE, Math.min(MoonSnowShape.MAX_ANGLE, angleDeg))));
        for (int d : DISTANCES) {
            double line = surfaceY + d * tan;
            if (line > surfaceY + 24.0D || line > 255.0D) break;
            int h = heights.at(d);
            if (h >= 0 && h > line) return true;
        }
        return false;
    }
}
