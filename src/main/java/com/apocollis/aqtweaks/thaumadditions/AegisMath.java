package com.apocollis.aqtweaks.thaumadditions;

/** Pure Aegis rules (no Minecraft types), kept apart so they can be unit tested. */
public final class AegisMath {
    private AegisMath() {}

    /** Vanilla's hurt window: damage inside it only counts if it is bigger than the last hit. */
    public static final int HURT_WINDOW_TICKS = 10;

    /** Damage the shield can take: {@code power * perLevel}, never above {@code maxPool}, never below 1. */
    public static float pool(int power, int perLevel, int maxPool) {
        int p = Math.max(1, power);
        return Math.max(1, Math.min(p * Math.max(1, perLevel), Math.max(1, maxPool)));
    }

    /** True on the tick a new vis interval starts (never on tick 0, which the raise itself pays). */
    public static boolean intervalDue(int heldTicks, int interval) {
        return interval > 0 && heldTicks > 0 && heldTicks % interval == 0;
    }

    /**
     * Vanilla's shield test: the source is in front when the look direction points towards it. All values are
     * horizontal; a source on the same spot is never in front.
     */
    public static boolean isFrontal(double lookX, double lookZ, double toSourceX, double toSourceZ) {
        return lookX * toSourceX + lookZ * toSourceZ > 0.0D;
    }

    /** One rune per {@code perRune} points of pool left, rounded up; none when the pool is empty. */
    public static int runes(float pool, int perRune) {
        if (pool <= 0.0F || perRune <= 0) return 0;
        return (int) Math.ceil(pool / perRune);
    }

    /**
     * The damage that counts for a blocked hit, matching vanilla's hurt window: a hit inside the window counts only
     * for what it adds over the last blocked hit, a hit outside it counts in full.
     */
    public static float effectiveDamage(float amount, float lastDamage, int ticksSinceLast) {
        if (amount <= 0.0F) return 0.0F;
        if (ticksSinceLast >= HURT_WINDOW_TICKS) return amount;
        return Math.max(0.0F, amount - lastDamage);
    }

    /** Pool left after a hit. A hit of at least the remaining pool breaks the shield and the rest is lost. */
    public static float remaining(float pool, float damage) {
        return Math.max(0.0F, pool - Math.max(0.0F, damage));
    }

    public static boolean breaks(float pool, float damage) {
        return damage > 0.0F && damage >= pool;
    }

    /** Complexity chosen so the focus's own cast cost (complexity / 5, in vis) equals the interval bill. */
    public static int complexityFor(double visPerInterval) {
        return Math.max(1, (int) Math.round(visPerInterval * 5.0D));
    }
}
