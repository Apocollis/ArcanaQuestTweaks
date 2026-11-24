package com.apocollis.aqtweaks.reskillable;

/**
 * Pure rules for the starter class perks Riposte, Steady Draw, Fleetfoot and Vein Rhythm (starter-class plan 13).
 * No Minecraft types, so the decisions are unit tested; {@link StarterClassPerks} wires them to events.
 */
public final class StarterPerkMath {

    /** Vanilla scales sneaking movement input by this factor (the "sneak penalty"). */
    public static final double VANILLA_SNEAK_FACTOR = 0.3D;

    private StarterPerkMath() {}

    // ---- Riposte -------------------------------------------------------------------------------------------

    /** A hit is a parry when a shield is up and was raised at most {@code windowTicks} ticks ago. */
    public static boolean isParry(int ticksRaised, int windowTicks) {
        return ticksRaised >= 0 && ticksRaised <= windowTicks;
    }

    // ---- Steady Draw ---------------------------------------------------------------------------------------

    /**
     * Whether the bow-hold stamina cost is billed on this use tick. Without the perk: every {@code interval} ticks.
     * With it: only after full draw, counted from the moment the bow reached full draw.
     */
    public static boolean billsBowHold(boolean steadyDraw, int ticksUsed, int interval, int fullDrawTicks) {
        if (interval <= 0 || ticksUsed <= 0) {
            return false;
        }
        if (!steadyDraw) {
            return ticksUsed % interval == 0;
        }
        return ticksUsed > fullDrawTicks && (ticksUsed - fullDrawTicks) % interval == 0;
    }

    /**
     * Whether an arrow that hit an entity comes back. Never for arrows that cannot be picked up (this also covers
     * Infinity-fired arrows, which vanilla marks creative-only) and never for arrow entities we cannot rebuild.
     *
     * @param roll uniform random in [0,1)
     */
    public static boolean returnsArrow(boolean pickupAllowed, boolean vanillaArrowEntity, double roll, double chance) {
        return pickupAllowed && vanillaArrowEntity && roll < chance;
    }

    // ---- Fleetfoot -----------------------------------------------------------------------------------------

    /** The first {@code freeTicks} ticks of a sprint run cost nothing. */
    public static boolean freeSprintTick(int runTicks, int freeTicks) {
        return freeTicks > 0 && runTicks < freeTicks;
    }

    /** A new sprint run gets the free start only after {@code rearmTicks} ticks of not sprinting. */
    public static boolean sprintFreeStartArmed(int idleTicks, int rearmTicks) {
        return idleTicks >= Math.max(0, rearmTicks);
    }

    /**
     * Attribute modifier amount (operation 2, multiply total) that gives back {@code restore} (0..1) of the vanilla
     * sneak slowdown: sneak speed goes from {@code s} to {@code s + restore * (1 - s)} of walking speed.
     */
    public static double sneakModifierAmount(double restore) {
        double r = Math.max(0.0D, Math.min(1.0D, restore));
        double target = VANILLA_SNEAK_FACTOR + r * (1.0D - VANILLA_SNEAK_FACTOR);
        return target / VANILLA_SNEAK_FACTOR - 1.0D;
    }

    // ---- Vein Rhythm ---------------------------------------------------------------------------------------

    /** Stack count after a qualifying break at {@code now}: continues a chain inside the window, else restarts at 1. */
    public static int stacksAfterBreak(int stacks, long lastBreak, long now, int windowTicks, int cap) {
        int capped = Math.max(1, cap);
        if (stacks > 0 && now - lastBreak <= windowTicks) {
            return Math.min(capped, stacks + 1);
        }
        return 1;
    }

    /** Stacks still active at {@code now} (they expire when the window passes with no break). */
    public static int activeStacks(int stacks, long lastBreak, long now, int windowTicks) {
        return stacks > 0 && now - lastBreak <= windowTicks ? stacks : 0;
    }

    /** Break speed multiplier for {@code stacks} stacks of {@code perStack} each. */
    public static float breakSpeedMultiplier(int stacks, double perStack) {
        return 1.0F + (float) (Math.max(0, stacks) * Math.max(0.0D, perStack));
    }
}
