package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.List;

/**
 * Decides what the Swarm Mage does about a hurt escort (plan 18.7). Pure: the entity class snapshots its escorts and its
 * own state, calls {@link #plan}, and carries out the answer.
 *
 * <p>The Healing Aura is a small cloud centred on the caster, so the mage has to be close: within
 * {@code castDistance} it casts, otherwise it blinks next to the ally, otherwise (blink on cooldown) it walks over unless a
 * player is already in melee range. The lowest health fraction below the threshold, inside the range, is served first.
 */
public final class AllyHealPlanner {

    public enum Action { NONE, CAST, BLINK, WALK }

    /** One escort as the planner sees it. {@code index} is the caller's own handle. */
    public static final class Ally {
        public final int index;
        public final double health;
        public final double maxHealth;
        /** Distance from the mage in blocks. */
        public final double distance;

        public Ally(int index, double health, double maxHealth, double distance) {
            this.index = index;
            this.health = health;
            this.maxHealth = maxHealth;
            this.distance = distance;
        }

        double fraction() {
            return maxHealth > 0.0D ? health / maxHealth : 1.0D;
        }
    }

    /** Tuning from the library ({@code healAlly*}). */
    public static final class Params {
        public double hpFraction = 0.6D;
        public double range = 16.0D;
        public double castDistance = 2.5D;
    }

    /** The mage's own state this tick. */
    public static final class State {
        public long now;
        /** Earliest tick the next Healing Aura may be cast (the heal cooldown). */
        public long healReadyAt;
        /** The Healing Aura focus is carried and off its own cooldown. */
        public boolean auraReady;
        public boolean blinkReady;
        public boolean stunned;
        /** A player is inside the melee trigger range, so walking away from them is not an option. */
        public boolean playerInMeleeRange;
    }

    /** The answer: an action and the {@link Ally#index} it applies to (-1 for NONE). */
    public static final class Decision {
        public final Action action;
        public final int allyIndex;

        Decision(Action action, int allyIndex) {
            this.action = action;
            this.allyIndex = allyIndex;
        }
    }

    private static final Decision NONE = new Decision(Action.NONE, -1);

    private AllyHealPlanner() {}

    public static Decision plan(List<Ally> allies, Params p, State s) {
        if (s.stunned || allies == null || allies.isEmpty()) {
            return NONE;
        }
        Ally best = null;
        for (Ally a : allies) {
            if (a.health <= 0.0D || a.maxHealth <= 0.0D || a.distance > p.range || a.fraction() >= p.hpFraction) {
                continue;
            }
            if (best == null || a.fraction() < best.fraction()
                    || (a.fraction() == best.fraction() && a.distance < best.distance)) {
                best = a;
            }
        }
        if (best == null) {
            return NONE;
        }
        if (best.distance <= p.castDistance) {
            return s.auraReady && s.now >= s.healReadyAt ? new Decision(Action.CAST, best.index) : NONE;
        }
        if (s.blinkReady) {
            return new Decision(Action.BLINK, best.index);
        }
        return s.playerInMeleeRange ? NONE : new Decision(Action.WALK, best.index);
    }
}
