package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * The yes/no decisions of the Battlemage's defence (plan 3.5, 3.10), pure so they can be tested without a world.
 * Priority order is the AI's: peel, then the defensive escape, then retreat and heal (plan 3.2).
 */
public final class DefenseRules {

    private DefenseRules() {}

    /**
     * The strict defensive escape (blink or rift): <b>all four</b> must hold.
     * <ol>
     * <li>health below {@code awayHpFraction};
     * <li>a player within {@code closeRange};
     * <li>the mage cannot get away on foot: the player has been that close for {@code pressureTicks}, or it is cornered;
     * <li>Knock Back is unavailable, or was used within the window and did not clear the player.
     * </ol>
     */
    public static boolean awayEscape(double hpFraction, double awayHpFraction, double playerDistance, double closeRange,
            int pressureTicks, int needPressureTicks, boolean cornered, boolean knockBackReady, long ticksSinceKnockBack,
            int knockBackWindowTicks) {
        if (hpFraction >= awayHpFraction) {
            return false;
        }
        if (playerDistance > closeRange) {
            return false;
        }
        if (!(pressureTicks >= needPressureTicks || cornered)) {
            return false;
        }
        boolean usedRecently = ticksSinceKnockBack >= 0 && ticksSinceKnockBack <= knockBackWindowTicks;
        return !knockBackReady || usedRecently;
    }

    /** Heal in place: hurt enough, a charge and the focus ready, and not in melee pressure (peel comes first). */
    public static boolean healInPlace(double hpFraction, double healHpFraction, boolean chargeAvailable, boolean focusReady,
            double playerDistance, double meleePressureRange) {
        return hpFraction <= healHpFraction && chargeAvailable && focusReady && playerDistance > meleePressureRange;
    }

    /** Start a retreat: at or below the retreat health, with a heal available, and not just done retreating. */
    public static boolean startRetreat(double hpFraction, double retreatHpFraction, boolean chargeAvailable, boolean cooledDown) {
        return hpFraction <= retreatHpFraction && chargeAvailable && cooledDown;
    }

    /** A retreat ends when healthy again, out of charges, or out of time. */
    public static boolean endRetreat(double hpFraction, double resumeFraction, boolean chargeAvailable, long ticksRetreating,
            int maxTicks) {
        return hpFraction >= resumeFraction || !chargeAvailable || ticksRetreating >= maxTicks;
    }

    /** Peel: a player within reach, Knock Back ready, its own cooldown over and no melee burst under way. */
    public static boolean peel(double playerDistance, double peelRange, boolean knockBackReady, boolean cooledDown,
            boolean burstActive) {
        return playerDistance <= peelRange && knockBackReady && cooledDown && !burstActive;
    }
}
