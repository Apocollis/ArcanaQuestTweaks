package com.apocollis.aqtweaks.twilightforest;

import java.util.Random;

/** Pure shape of the Ley-Rupture particles: sprites, colours, ranges, spread and budgets. No Minecraft types. */
public final class LeyEruptionShape {
    private LeyEruptionShape() {}

    /** Share of {@code Max Ley Particles} the vent plumes may use; the ground passes get the rest. */
    public static final double VENT_SHARE = 0.40D;
    /** Reference area of the old spawn box, in blocks per side. */
    public static final double BASE_BOX = 32.0D;
    public static final double WARNING_KEEP_OUT = 4.0D;
    public static final double VENT_KEEP_OUT = 8.0D;

    // ---- sprites (particles.png index = row * 16 + column) ----

    public static final int GLYPH_MIN = 225;
    public static final int GLYPH_MAX = 250;

    public static int glyphSprite(Random r) {
        return GLYPH_MIN + r.nextInt(GLYPH_MAX - GLYPH_MIN + 1);
    }

    /** Wisps 128..135 (half), diamond and cross sparks 164..167 (a quarter), solid dots 176..180 (a quarter). */
    public static int eruptionSprite(Random r) {
        int pick = r.nextInt(4);
        if (pick < 2) return 128 + r.nextInt(8);
        if (pick == 2) return 164 + r.nextInt(4);
        return 176 + r.nextInt(5);
    }

    // ---- colour: 70 percent violet, 30 percent emerald ----

    public static final int VIOLET_LOW = 0x8A2BE2;
    public static final int VIOLET_HIGH = 0xC44DFF;
    public static final int EMERALD_LOW = 0x1FC97A;
    public static final int EMERALD_HIGH = 0x48FF9E;

    public static boolean isEmerald(int rgb) {
        return ((rgb >> 8) & 0xFF) > ((rgb >> 16) & 0xFF);
    }

    public static int colour(Random r) {
        float t = r.nextFloat();
        return r.nextFloat() < 0.70F ? mix(VIOLET_LOW, VIOLET_HIGH, t) : mix(EMERALD_LOW, EMERALD_HIGH, t);
    }

    private static int mix(int a, int b, float t) {
        int rr = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int gg = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bb = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (rr << 16) | (gg << 8) | bb;
    }

    // ---- lifetimes and speeds ----

    public static int glyphLifetime(Random r) {
        return 36 + r.nextInt(13);
    }

    public static int eruptionLifetime(Random r) {
        return 22 + r.nextInt(15);
    }

    public static double glyphRise(Random r, boolean active) {
        return active ? 0.24D + r.nextDouble() * 0.18D : 0.16D + r.nextDouble() * 0.12D;
    }

    public static double eruptionRise(Random r) {
        return 0.30D + r.nextDouble() * 0.42D;
    }

    /** Fade in over the first tenth of the life and out over the last 35 percent. 0..1. */
    public static float alpha(int age, int maxAge) {
        if (maxAge <= 0) return 0.0F;
        float life = Math.max(0.0F, Math.min(1.0F, (float) age / (float) maxAge));
        if (life < 0.10F) return life / 0.10F;
        if (life > 0.65F) return (1.0F - life) / 0.35F;
        return 1.0F;
    }

    // ---- spread ----

    /** Area factor against the old 32 by 32 box: holds the density per square block when the radius grows. */
    public static double areaFactor(double radius) {
        double f = 2.0D * radius / BASE_BOX;
        return f * f;
    }

    /** Linear factor, used for the vent count (each plume is a cluster). */
    public static double linearFactor(double radius) {
        return 2.0D * radius / BASE_BOX;
    }

    /** Spawn attempts this tick for a pass whose base count is for the old box. */
    public static int attempts(double scale, double base, double radius, double density) {
        return Math.max(0, (int) (scale * base * areaFactor(radius) * density));
    }

    public static int vents(double scale, double radius, double density) {
        return Math.max(0, (int) Math.round((1 + (int) (scale * 3.0D)) * linearFactor(radius) * density));
    }

    /**
     * An offset from the player, uniform over the square of half-width {@code radius}, never closer than
     * {@code keepOut} (Euclidean). A few redraws, then the point is pushed out along its direction, so the rate holds.
     */
    public static double[] offset(Random r, double radius, double keepOut) {
        double dx = 0.0D;
        double dz = 0.0D;
        for (int i = 0; i < 6; i++) {
            dx = (r.nextDouble() * 2.0D - 1.0D) * radius;
            dz = (r.nextDouble() * 2.0D - 1.0D) * radius;
            if (dx * dx + dz * dz >= keepOut * keepOut) return new double[] {dx, dz};
        }
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-6D) {
            dx = keepOut;
            dz = 0.0D;
        } else {
            dx = dx / len * keepOut;
            dz = dz / len * keepOut;
        }
        return new double[] {dx, dz};
    }

    /** 1.0 within 8 blocks, growing linearly to 1.6 at the radius. Monotonic. */
    public static float distanceScale(double dist, double radius) {
        if (dist <= 8.0D || radius <= 8.0D) return 1.0F;
        double t = Math.min(1.0D, (dist - 8.0D) / (radius - 8.0D));
        return (float) (1.0D + 0.6D * t);
    }

    // ---- budgets ----

    /** A vent bolt may spawn while vents are under their share and the whole cap is not reached. */
    public static boolean ventAllowed(int vent, int total, int cap) {
        return total < cap && vent < (int) (cap * VENT_SHARE);
    }

    /** A ground particle may spawn while the ground count is under its share and the whole cap is not reached. */
    public static boolean groundAllowed(int vent, int total, int cap) {
        return total < cap && (total - vent) < (int) (cap * (1.0D - VENT_SHARE));
    }
}
