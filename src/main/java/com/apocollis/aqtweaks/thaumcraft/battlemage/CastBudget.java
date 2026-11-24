package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * The Battlemage's cast budget (plan 3.11 item 1, 3.13 item 3). A mob pays no vis, so cooldowns alone would make it a
 * metronome. Every budgeted cast spends {@code complexity * costPerComplexity} energy. When the pool runs down to
 * {@code holdBelowFraction} of its maximum the mage must hold back until it refills to {@code resumeFraction}: bursts of
 * three or four casts, then a visible opening. Pure and unit tested; time is passed in as the entity's tick count.
 */
public final class CastBudget {

    private final double max;
    private final double baseRegenPerTick;
    private double regenScale = 1.0D;
    private final double holdBelow;
    private final double resumeAt;
    private double energy;
    private boolean holding;
    private long lastTick = -1L;

    public CastBudget(double max, double regenPerSecond, double holdBelowFraction, double resumeFraction) {
        this.max = Math.max(1.0D, max);
        this.baseRegenPerTick = Math.max(0.0D, regenPerSecond) / 20.0D;
        this.holdBelow = this.max * clamp(holdBelowFraction);
        this.resumeAt = this.max * Math.max(clamp(holdBelowFraction), clamp(resumeFraction));
        this.energy = this.max;
    }

    private static double clamp(double f) {
        return Math.max(0.0D, Math.min(1.0D, f));
    }

    /** Adds the energy earned since the last call and ends the hold once the pool has refilled enough. */
    public void regen(long now) {
        if (lastTick < 0L) {
            lastTick = now;
        }
        long dt = now - lastTick;
        if (dt > 0L) {
            energy = Math.min(max, energy + dt * baseRegenPerTick * regenScale);
            lastTick = now;
        }
        if (holding && energy >= resumeAt) {
            holding = false;
        }
    }

    /** Phase scaling of the regeneration (plan 3.13: Empowered 1.25, Desperate 1.5). */
    public void setRegenScale(double scale) {
        this.regenScale = Math.max(0.0D, scale);
    }

    /** A cost above the maximum is capped to it, so the heaviest focus can still be cast from a full pool. */
    public boolean canAfford(double cost) {
        return !holding && energy >= Math.min(cost, max);
    }

    public void spend(double cost) {
        energy = Math.max(0.0D, energy - Math.min(cost, max));
        if (energy <= holdBelow) {
            holding = true;
        }
    }

    public double energy() {
        return energy;
    }

    public double max() {
        return max;
    }

    public boolean holding() {
        return holding;
    }
}
