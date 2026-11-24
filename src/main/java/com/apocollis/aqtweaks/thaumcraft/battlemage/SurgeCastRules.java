package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The low-health rule for a Battlemage overchanneling in a Ley-Rupture surge (plan 6.15): below the threshold it may
 * only cast healing foci, blink and rift, plus melee. Every other cast would cost it backlash damage it cannot afford.
 * Pure and unit tested.
 */
public final class SurgeCastRules {

    private static final Set<String> ALLOWED_ROLES = new HashSet<>(Arrays.asList("heal_self", "heal_ally", "blink", "rift"));

    private SurgeCastRules() {}

    /** True when the mage is below the threshold while overchanneling, so only the allowed moves remain. */
    public static boolean restricted(boolean overchanneling, double healthFraction, double threshold) {
        return overchanneling && threshold > 0.0D && healthFraction < threshold;
    }

    /** May a focus of this role be cast right now? */
    public static boolean castAllowed(String role, boolean restricted) {
        return !restricted || (role != null && ALLOWED_ROLES.contains(role));
    }
}
