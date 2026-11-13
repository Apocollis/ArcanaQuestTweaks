package com.apocollis.aqtweaks.twilightforest;

import java.util.function.IntPredicate;

/**
 * The surface-relative height rule of the Ley-Rupture. The reference point is the average ground surface around
 * the player minus an offset; the exposure factor falls linearly from 1 at or below it to 0 at {@code fadeHeight}
 * above it. Standing on a branch counts the ground below the canopy, not the leaf under the feet. No Minecraft
 * types: the world adapter supplies "is this Y ground?".
 */
public final class GroundReference {
    private GroundReference() {}

    /** First Y at or below {@code startY} where {@code isGround} is true, scanning down to {@code minY}; minY if none. */
    public static int surfaceY(int startY, int minY, IntPredicate isGround) {
        for (int y = startY; y >= minY; y--) {
            if (isGround.test(y)) return y;
        }
        return minY;
    }

    /** Average of the surface heights (the player's column and the four at 8 blocks) minus the offset. */
    public static double reference(int[] surfaceYs, int offset) {
        if (surfaceYs.length == 0) return 0.0D;
        double sum = 0.0D;
        for (int y : surfaceYs) sum += y;
        return sum / surfaceYs.length - offset;
    }

    /** 1 at or below the reference, 0 at or above {@code fadeHeight} over it, linear between. */
    public static double exposure(double playerY, double reference, double fadeHeight) {
        double height = playerY - reference;
        double fade = Math.max(1.0D, fadeHeight);
        return Math.max(0.0D, Math.min(1.0D, 1.0D - height / fade));
    }
}
