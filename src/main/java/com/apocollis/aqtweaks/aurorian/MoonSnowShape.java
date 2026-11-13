package com.apocollis.aqtweaks.aurorian;

import java.util.Random;

/** Pure shape of the Moonfrost blizzard particles: classes, sprites, sizes, speeds, fall and the perimeter. No Minecraft types. */
public final class MoonSnowShape {
    private MoonSnowShape() {}

    public enum SnowClass {
        SPECK, SMALL, MEDIUM, PELLET, PUFF, CLUMP, STREAK
    }

    /** Sprite index sets on particles.png (index = row * 16 + column). */
    public static int sprite(SnowClass c, Random r) {
        switch (c) {
            case SPECK:
                return 113;
            case SMALL:
                return 160 + r.nextInt(4);
            case MEDIUM:
                return 164 + r.nextInt(4);
            case PELLET:
                return 176 + r.nextInt(5);
            case PUFF:
                return 5 + r.nextInt(3);
            case STREAK:
                return 114;
            default:
                return 0; // clumps use the snow block texture, not a sprite index
        }
    }

    public static boolean inSet(SnowClass c, int sprite) {
        switch (c) {
            case SPECK:
                return sprite == 113;
            case SMALL:
                return sprite >= 160 && sprite <= 163;
            case MEDIUM:
                return sprite >= 164 && sprite <= 167;
            case PELLET:
                return sprite >= 176 && sprite <= 180;
            case PUFF:
                return sprite >= 5 && sprite <= 7;
            case STREAK:
                return sprite == 114;
            default:
                return true;
        }
    }

    // ---- class mix ----

    private static final SnowClass[] ORDER = SnowClass.values();
    private static final double[] BASE = {25, 25, 15, 10, 15, 7, 3};
    /** Added at full strength: heavier snow shifts toward puffs and clumps and away from the fine grains. */
    private static final double[] HEAVY = {-10, -8, 0, 0, 10, 5, 3};

    public static double[] weights(double strength) {
        double s = Math.max(0.0D, Math.min(1.0D, strength));
        double[] w = new double[ORDER.length];
        double total = 0.0D;
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.max(0.5D, BASE[i] + HEAVY[i] * s);
            total += w[i];
        }
        for (int i = 0; i < w.length; i++) w[i] /= total;
        return w;
    }

    public static SnowClass pick(Random r, double strength, boolean streaks) {
        double[] w = weights(strength);
        double roll = r.nextDouble();
        double acc = 0.0D;
        SnowClass out = SnowClass.SMALL;
        for (int i = 0; i < w.length; i++) {
            acc += w[i];
            if (roll < acc) {
                out = ORDER[i];
                break;
            }
        }
        if (out == SnowClass.STREAK && !streaks) return SnowClass.SMALL;
        return out;
    }

    /** The veil around the player: mostly puffs, with some flakes. Never a clump or a streak. */
    public static SnowClass pickShell(Random r) {
        float roll = r.nextFloat();
        if (roll < 0.45F) return SnowClass.PUFF;
        if (roll < 0.65F) return SnowClass.SMALL;
        if (roll < 0.80F) return SnowClass.MEDIUM;
        if (roll < 0.90F) return SnowClass.PELLET;
        return SnowClass.SPECK;
    }

    // ---- size, alpha, speed, life per class ----

    public static float scale(SnowClass c, Random r) {
        switch (c) {
            case SPECK:
                return 0.6F + r.nextFloat() * 0.6F;
            case SMALL:
                return 0.7F + r.nextFloat() * 0.5F;
            case MEDIUM:
                return 1.4F + r.nextFloat() * 1.2F;
            case PELLET:
                return 0.8F + r.nextFloat() * 1.2F;
            case PUFF:
                return 2.8F + r.nextFloat() * 3.7F;
            case CLUMP:
                return 0.75F + r.nextFloat() * 1.0F;
            default:
                return 2.2F + r.nextFloat() * 2.2F;
        }
    }

    public static float alpha(SnowClass c, Random r, double strength) {
        float boost = (float) (0.1D * Math.max(0.0D, Math.min(1.0D, strength)));
        float a;
        switch (c) {
            case SPECK:
                a = 0.70F + r.nextFloat() * 0.25F;
                break;
            case SMALL:
                a = 0.60F + r.nextFloat() * 0.30F;
                break;
            case MEDIUM:
                a = 0.45F + r.nextFloat() * 0.30F;
                break;
            case PELLET:
                a = 0.60F + r.nextFloat() * 0.30F;
                break;
            case PUFF:
                a = 0.12F + r.nextFloat() * 0.18F;
                break;
            case CLUMP:
                a = 0.90F;
                break;
            default:
                a = 0.35F + r.nextFloat() * 0.25F;
                break;
        }
        return Math.min(1.0F, a + boost);
    }

    public static float speedFactor(SnowClass c) {
        switch (c) {
            case SPECK:
                return 1.15F;
            case SMALL:
                return 1.10F;
            case MEDIUM:
                return 0.90F;
            case PELLET:
                return 1.00F;
            case PUFF:
                return 0.60F;
            case CLUMP:
                return 1.25F;
            default:
                return 1.35F;
        }
    }

    public static int lifetime(SnowClass c, Random r, boolean perimeter) {
        if (perimeter) return 14 + r.nextInt(7);
        if (c == SnowClass.PUFF) return 26 + r.nextInt(11);
        return 20 + r.nextInt(9);
    }

    /** Streaks and clumps are single particles that must read as fast; puffs are the only depth-test-off class. */
    public static boolean disablesDepth(SnowClass c) {
        return c == SnowClass.PUFF;
    }

    // ---- fall ----

    public static final double MIN_ANGLE = 4.0D;
    public static final double MAX_ANGLE = 82.0D;

    /** Fall angle in degrees after the per-particle jitter (about +/-15 percent), clamped to [4, 82]. */
    public static double jitteredAngle(double angleDeg, double jitter01) {
        double a = angleDeg * (1.0D + (jitter01 * 2.0D - 1.0D) * 0.15D);
        return Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, a));
    }

    /** Vertical speed (blocks per tick, positive downward) = horizontal speed times tan of the clamped angle. */
    public static double fallPerTick(double horizontalSpeed, double angleDeg, double jitter01) {
        return horizontalSpeed * Math.tan(Math.toRadians(jitteredAngle(angleDeg, jitter01)));
    }

    /** Horizontal speed in blocks per tick: 0.7 at the start of the warning to about 1.6 at full strength. */
    public static double horizontalSpeed(double scale, double multiplier) {
        return (0.7D + 0.9D * Math.max(0.0D, Math.min(1.0D, scale))) * multiplier;
    }

    /** Slow gust pulse on world time, 0.85 to 1.25, applied to horizontal and vertical speed together. */
    public static double gust(long worldTime) {
        return 1.05D + 0.20D * Math.sin(worldTime * 0.045D);
    }

    // ---- perimeter ----

    /**
     * Radius from a triangular distribution that peaks at {@code r}: minimum {@code max(3, 0.7 r)}, maximum
     * {@code max(min, r + 8)}.
     */
    public static double perimeterRadius(Random rnd, double r) {
        double min = Math.max(3.0D, 0.7D * r);
        double max = Math.max(min, r + 8.0D);
        double peak = Math.max(min, Math.min(max, r));
        double u = rnd.nextDouble();
        double cut = max > min ? (peak - min) / (max - min) : 0.5D;
        if (max <= min) return min;
        if (u < cut) return min + Math.sqrt(u * (max - min) * (peak - min));
        return max - Math.sqrt((1.0D - u) * (max - min) * (max - peak));
    }

    /** Unit vector, uniform on the sphere then flattened vertically so it is not mostly sky and ground. */
    public static double[] perimeterDirection(Random rnd) {
        double z = rnd.nextDouble() * 2.0D - 1.0D;
        double t = rnd.nextDouble() * Math.PI * 2.0D;
        double rr = Math.sqrt(Math.max(0.0D, 1.0D - z * z));
        double x = rr * Math.cos(t);
        double y = z * 0.7D;
        double zz = rr * Math.sin(t);
        double len = Math.sqrt(x * x + y * y + zz * zz);
        return new double[] {x / len, y / len, zz / len};
    }

    // ---- roofs and doorways ----

    /** True when a flake at {@code posY} is under a roof whose cover height is {@code coverY} (-1 = unknown, never killed). */
    public static boolean underRoof(double posY, int coverY) {
        return coverY >= 0 && posY < coverY - 0.25D;
    }

    /** Moves the particle-only exposure ease one client tick (0.115 per tick settles 90 percent in about a second). */
    public static float easeExposure(float current, float target) {
        return current + (target - current) * 0.115F;
    }
}
