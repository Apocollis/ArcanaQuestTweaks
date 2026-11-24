package com.apocollis.aqtweaks.thaumcraft.battlemage;

/** The pure counting rule of the Spell Bat upkeep (plan 3.12): how many bats to keep, and when to recast. */
public final class SummonUpkeep {

    private SummonUpkeep() {}

    /**
     * @param configured {@code desiredActiveSpellBats} from the library; 0 or less means auto
     * @param castSize bats one cast of the mage's summon focus spawns (1 for Curse Bat, 5 for Spell Bat Swarm)
     */
    public static int desired(int configured, int castSize) {
        if (configured > 0) {
            return configured;
        }
        return Math.max(1, (Math.max(1, castSize) + 1) / 2);
    }

    /** Recast when fewer bats are alive than wanted: a Curse Bat mage at zero, a Swarm mage at 2 or fewer. */
    public static boolean needed(int active, int desired) {
        return active < desired;
    }
}
