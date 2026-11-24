package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * The miniboss phases of the Battlemage (plan 3.13 item 2). Pure and unit tested. Phase 0 is normal, 1 "Empowered"
 * below 66% health, 2 "Desperate" below 33%. A phase only ever goes up (a heal never takes the mage back to a calmer
 * phase); the reset rule clears it.
 */
public final class BattlemagePhase {

    public static final int NORMAL = 0;
    public static final int EMPOWERED = 1;
    public static final int DESPERATE = 2;

    private BattlemagePhase() {}

    /** The phase for this health fraction, never lower than {@code current}. */
    public static int next(int current, double healthFraction, double empoweredBelow, double desperateBelow) {
        int phase = NORMAL;
        if (healthFraction < desperateBelow) {
            phase = DESPERATE;
        } else if (healthFraction < empoweredBelow) {
            phase = EMPOWERED;
        }
        return Math.max(current, phase);
    }

    /** Cooldown multiplier per phase (smaller is faster). */
    public static double cooldownScale(int phase, double empowered, double desperate) {
        return phase >= DESPERATE ? desperate : (phase >= EMPOWERED ? empowered : 1.0D);
    }

    /** Cast budget regeneration multiplier per phase. */
    public static double regenScale(int phase, double empowered, double desperate) {
        return phase >= DESPERATE ? desperate : (phase >= EMPOWERED ? empowered : 1.0D);
    }

    /** Extra Spell Bats the mage keeps up. */
    public static int extraBats(int phase, int empowered, int desperate) {
        return phase >= DESPERATE ? desperate : (phase >= EMPOWERED ? empowered : 0);
    }

    /** Blink cooldown: Desperate caps it at {@code desperateCap}. */
    public static int blinkCooldown(int phase, int base, int desperateCap) {
        return phase >= DESPERATE && desperateCap > 0 ? Math.min(base, desperateCap) : base;
    }

    /** Heal charge regeneration interval: shorter in Desperate (factor below 1). */
    public static int healRegenTicks(int phase, int base, double desperateFactor) {
        return phase >= DESPERATE ? Math.max(1, (int) Math.round(base * desperateFactor)) : base;
    }
}
