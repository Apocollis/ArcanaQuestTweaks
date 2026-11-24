package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * The heal charge pool (plan 3.12): a small regenerating count, separate from the cast budget. Each heal cast spends one
 * charge; one charge returns every {@code regenTicks}. Starts full. Pure.
 */
public final class HealBudget {

    private final int max;
    private final int baseRegenTicks;
    private int regenTicks;
    private int charges;
    private long nextChargeAt;

    public HealBudget(int max, int regenTicks) {
        this.max = Math.max(0, max);
        this.baseRegenTicks = Math.max(1, regenTicks);
        this.regenTicks = this.baseRegenTicks;
        this.charges = this.max;
    }

    /** Phase scaling: a new interval for later charges (Desperate regenerates faster). */
    public void setRegenTicks(int ticks) {
        this.regenTicks = Math.max(1, ticks);
    }

    public int baseRegenTicks() {
        return baseRegenTicks;
    }

    /** Brings regeneration up to date. Call every tick or every decision tick. */
    public void tick(long now) {
        if (charges >= max) {
            nextChargeAt = now + regenTicks;
            return;
        }
        while (charges < max && now >= nextChargeAt) {
            charges++;
            nextChargeAt += regenTicks;
        }
    }

    public boolean canHeal() {
        return charges > 0;
    }

    public int charges() {
        return charges;
    }

    public int max() {
        return max;
    }

    /** Spends one charge; the regeneration clock starts when the pool was full. */
    public boolean spend(long now) {
        if (charges <= 0) {
            return false;
        }
        if (charges >= max) {
            nextChargeAt = now + regenTicks;
        }
        charges--;
        return true;
    }
}
