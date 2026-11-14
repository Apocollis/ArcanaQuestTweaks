package com.apocollis.aqtweaks.nether;

import java.util.Random;

/**
 * Where a spout's thrown lava aims: a distance of 3 to 12 blocks from the spout and, most of the time, an angle
 * inside an arc around the direction of the player so the lava lands on the bank they are working on. No Minecraft
 * types.
 */
public final class SplashAim {
    private SplashAim() {}

    public static final double MIN_DISTANCE = 3.0D;
    public static final double MAX_DISTANCE = 12.0D;
    /** Half width of the arc around the player direction, in radians (75 degrees). */
    public static final double ARC = Math.toRadians(75.0D);
    /** Share of throws aimed inside the arc; the rest go in any direction. */
    public static final double BIAS = 0.6D;

    /** Horizontal throw distance in [MIN_DISTANCE, MAX_DISTANCE). */
    public static double distance(Random rand) {
        return MIN_DISTANCE + rand.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
    }

    /** Throw angle in radians. {@code towardPlayer} is NaN when there is no player to favour. */
    public static double angle(Random rand, double towardPlayer) {
        if (!Double.isNaN(towardPlayer) && rand.nextDouble() < BIAS) {
            return towardPlayer + (rand.nextDouble() * 2.0D - 1.0D) * ARC;
        }
        return rand.nextDouble() * Math.PI * 2.0D;
    }

    /** True if {@code angle} lies within the arc around {@code toward}. */
    public static boolean inArc(double angle, double toward) {
        double diff = Math.abs(angle - toward) % (Math.PI * 2.0D);
        if (diff > Math.PI) diff = Math.PI * 2.0D - diff;
        return diff <= ARC + 1.0E-9D;
    }
}
