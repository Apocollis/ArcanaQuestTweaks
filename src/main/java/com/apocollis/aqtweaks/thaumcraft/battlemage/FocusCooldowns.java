package com.apocollis.aqtweaks.thaumcraft.battlemage;

/** Natural focus cooldown (plan 3.15), read from {@code ItemFocus.getActivationTime}: {@code max(5, (c / 5) * (c / 4))} ticks. */
public final class FocusCooldowns {

    private FocusCooldowns() {}

    public static int natural(int complexity) {
        return Math.max(5, (complexity / 5) * (complexity / 4));
    }

    /** Override (when non-null) or natural cooldown, times the config scale and the phase scale, at least 1 tick. */
    public static int effective(Integer override, int complexity, double configScale, double phaseScale) {
        int base = override != null ? Math.max(0, override) : natural(complexity);
        double scaled = base * Math.max(0.0D, configScale) * Math.max(0.0D, phaseScale);
        return Math.max(1, (int) Math.round(scaled));
    }
}
