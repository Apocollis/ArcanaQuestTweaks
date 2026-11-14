package com.apocollis.aqtweaks.weather;

/**
 * Wind shadow of the falling snow, from the height map only. A column is in shadow when a blocker upwind rises above
 * the slanted flight line to its surface. Shared by the Moonfrost snow and the Atum sand. No Minecraft types.
 */
public final class LayerShadow {
    private LayerShadow() {}

    /** Flight angle limits in degrees, so tan never flips sign or blows up near 90. */
    public static final double MIN_ANGLE = 4.0D;
    public static final double MAX_ANGLE = 82.0D;

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
        double tan = Math.tan(Math.toRadians(Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, angleDeg))));
        for (int d : DISTANCES) {
            double line = surfaceY + d * tan;
            if (line > surfaceY + 24.0D || line > 255.0D) break;
            int h = heights.at(d);
            if (h >= 0 && h > line) return true;
        }
        return false;
    }
}
