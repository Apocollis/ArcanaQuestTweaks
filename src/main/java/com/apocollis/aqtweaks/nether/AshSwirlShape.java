package com.apocollis.aqtweaks.nether;

import java.util.Random;

/**
 * The look and motion of an ash cloud as pure functions of the normalised radius {@code d} (0 at the centre, 1 at
 * the edge of the reach ellipsoid): a dense core with a feathered rim, a slow spin about the vertical axis (fast in
 * the core) and a gentle roll (core rises, rim sinks) so the mass reads as a turning rounded cloud. No Minecraft types.
 */
public final class AshSwirlShape {
    private AshSwirlShape() {}

    public enum Kind { SHEET, FLECK, EMBER }

    /** Radius where the core stops being fully dense. */
    public static final double CORE = 0.55D;

    /** Normalised radius of an offset from the cloud centre. */
    public static double radius(double ox, double oy, double oz, double reachH, double reachV) {
        double h = Math.max(1.0E-6D, reachH);
        double v = Math.max(1.0E-6D, reachV);
        return Math.sqrt((ox / h) * (ox / h) + (oy / v) * (oy / v) + (oz / h) * (oz / h));
    }

    /** Spawn density: 1 in the core, easing to 0 at the edge. */
    public static double density(double d) {
        if (d <= CORE) return 1.0D;
        if (d >= 1.0D) return 0.0D;
        double t = (1.0D - d) / (1.0D - CORE);
        return t * t * (3.0D - 2.0D * t);
    }

    /** Alpha and size fall off with the same curve near the rim (never fully to zero until the edge). */
    public static float edgeFade(double d) {
        return (float) Math.max(0.0D, Math.min(1.0D, density(d) * 0.85D + 0.15D * (d < 1.0D ? 1.0D : 0.0D)));
    }

    /** Spin about the vertical axis, radians per tick: fastest in the core, half as fast at the rim. */
    public static double spinRate(double d, double spinSpeed) {
        double c = Math.max(0.0D, Math.min(1.0D, d));
        return spinSpeed * 0.05D * (1.0D - 0.5D * c);
    }

    /** Vertical roll in blocks per tick: the core rises, the rim sinks. */
    public static double roll(double d) {
        double c = Math.max(0.0D, Math.min(1.0D, d));
        return 0.05D * (1.0D - 2.0D * c);
    }

    /** About 28 percent soft sheets, 66 percent soot flecks and 6 percent hot cinders. */
    public static Kind kind(Random rand) {
        double r = rand.nextDouble();
        if (r < 0.28D) return Kind.SHEET;
        if (r < 0.94D) return Kind.FLECK;
        return Kind.EMBER;
    }

    public static float scale(Kind kind, Random rand) {
        switch (kind) {
            case SHEET:
                return 3.2F + rand.nextFloat() * 2.0F;
            case EMBER:
                return 0.35F + rand.nextFloat() * 0.3F;
            default:
                return 0.55F + rand.nextFloat() * 0.6F;
        }
    }

    public static float alpha(Kind kind, Random rand) {
        switch (kind) {
            case SHEET:
                return 0.30F + rand.nextFloat() * 0.35F;
            case EMBER:
                return 0.9F;
            default:
                return 0.55F + rand.nextFloat() * 0.35F;
        }
    }

    public static int lifetime(Kind kind, Random rand) {
        return kind == Kind.SHEET ? 70 + rand.nextInt(50) : 45 + rand.nextInt(45);
    }

    /** Smoky charcoal for sheets and flecks, orange-red for cinders. Returns packed 0xRRGGBB. */
    public static int colour(Kind kind, Random rand) {
        float v = rand.nextFloat();
        switch (kind) {
            case SHEET:
                return pack(0.16F + 0.10F * v, 0.14F + 0.08F * v, 0.13F + 0.07F * v);
            case EMBER:
                return pack(0.95F, 0.38F + 0.17F * v, 0.10F);
            default:
                return pack(0.08F + 0.10F * v, 0.08F + 0.10F * v, 0.08F + 0.10F * v);
        }
    }

    private static int pack(float r, float g, float b) {
        return (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
    }

    /** Wind direction (radians) of the warning wisps, fixed by the event seed. */
    public static double warningAngle(long seed) {
        long z = seed * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = z ^ (z >>> 31);
        return ((z >>> 11) * (1.0D / (double) (1L << 53))) * Math.PI * 2.0D;
    }

    /** Warning wisps per tick: none at the start, about 14 at the end of the warning (40 percent of a full storm's feel). */
    public static int warningWisps(float warning) {
        float w = Math.max(0.0F, Math.min(1.0F, warning));
        return Math.round(w * 14.0F);
    }
}
