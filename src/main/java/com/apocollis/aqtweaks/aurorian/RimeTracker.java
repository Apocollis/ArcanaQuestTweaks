package com.apocollis.aqtweaks.aurorian;

/**
 * Rime (freeze in place) arithmetic for the Moonfrost storm. No Minecraft types.
 *
 * <p>Rime is a value in 0..{@link #MAX}. Once a second it rises while the player stands still and unsheltered,
 * and falls when they move (2 per second), shelter (2) or warm up at a fire (3). Moving always thaws, and the
 * "still" threshold is relative to the current movement speed, so Slowness can never make thawing impossible.
 */
public final class RimeTracker {
    public static final double MAX = 15.0D;
    public static final double DECAY_MOVING = 2.0D;
    public static final double DECAY_SHELTER = 2.0D;
    public static final double DECAY_WARMTH = 3.0D;
    /** Vanilla walk speed attribute 0.1 corresponds to about 4.3 blocks per second. */
    public static final double BLOCKS_PER_SECOND_PER_SPEED = 43.0D;

    private RimeTracker() {}

    /** Horizontal blocks per second below which the player counts as standing still. */
    public static double stillThreshold(double speedAttribute, double factor) {
        return Math.max(0.0D, factor) * Math.max(0.0D, speedAttribute) * BLOCKS_PER_SECOND_PER_SPEED;
    }

    public static boolean isMoving(double movedBlocksInSecond, double speedAttribute, double factor) {
        return movedBlocksInSecond >= stillThreshold(speedAttribute, factor);
    }

    /** One second of rime. {@code gain} is the per-second gain of the player's gear tier. */
    public static double step(double rime, boolean moving, boolean sheltered, boolean warmth, double gain) {
        double next;
        if (warmth) {
            next = rime - DECAY_WARMTH;
        } else if (sheltered) {
            next = rime - DECAY_SHELTER;
        } else if (moving) {
            next = rime - DECAY_MOVING;
        } else {
            next = rime + Math.max(0.0D, gain);
        }
        return Math.max(0.0D, Math.min(MAX, next));
    }

    public static int stack(double rime) {
        return (int) Math.floor(Math.max(0.0D, Math.min(MAX, rime)) + 1.0E-9D);
    }

    /** Slowness amplifier for a stack, or -1 for none. 5+ I, 10+ III, 15 V. */
    public static int slownessAmplifier(int stack) {
        if (stack >= 15) return 4;
        if (stack >= 10) return 2;
        if (stack >= 5) return 0;
        return -1;
    }

    /** Mining Fatigue amplifier for a stack, or -1 for none. 10+ I, 15 III. */
    public static int fatigueAmplifier(int stack) {
        if (stack >= 15) return 2;
        if (stack >= 10) return 0;
        return -1;
    }

    /** At the maximum stack the frostbite damage is doubled. */
    public static boolean doublesDamage(int stack) {
        return stack >= 15;
    }
}
