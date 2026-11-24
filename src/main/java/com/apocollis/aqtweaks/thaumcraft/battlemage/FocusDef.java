package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.List;

/** One entry of the focus library (plan section 4). Plain data, filled by {@link LibraryParser}. */
public final class FocusDef {
    public String id;
    public int minTier = 1;
    public String role;
    public String slot = "any";
    public int complexity;
    public double baseWeight = 1.0D;
    public int[] dimensions = new int[0];
    public double minRange = 0.0D;
    public double maxRange = 24.0D;
    public boolean requireLos = true;
    /** Tag for what state this focus sets up (for example slow, stun, fire), or null. */
    public String opener;
    /** Target states this focus is a payoff for (stunned, slowed, wet, burning). */
    public List<String> followUp = new ArrayList<>();
    /** Null means the natural cooldown from the complexity. */
    public Integer cooldownTicks;
    public int windupTicks;
    /** Bats one cast of a summon_upkeep focus spawns (Curse Bat 1, Spell Bat Swarm 5); drives the upkeep count. */
    public int summonCount = 1;
    /** A cloud focus is not recast while this mage still owns a cloud (plan 3.12, refire on expire). */
    public boolean refireOnExpire;
    /** AI-performed (Blink, Rift): kept in the loadout but never cast through the focus engine. */
    public boolean performed;
    /** Optional drop-weight override (plan 13f); null uses the rule by slot and tier. */
    public Double dropWeight;
    /** CraftTweaker bracket line (see {@link CtFocusLine}). */
    public String nbt;

    public boolean castable() {
        return !performed && nbt != null;
    }
}
