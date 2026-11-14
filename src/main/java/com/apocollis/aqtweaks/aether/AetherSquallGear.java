package com.apocollis.aqtweaks.aether;

import java.util.Random;

/**
 * Gear rules of the Aether lightning storm as pure functions of what the player wears. Wind push is not affected by
 * conductive or insulated gear (heavy gear handles push, see the hazard gear plan). No Minecraft types.
 */
public final class AetherSquallGear {
    private AetherSquallGear() {}

    /** How a player's gear treats lightning, checked in this order. */
    public enum GearClass {
        /** A full channeling set: locked onto like conductive gear, but takes no damage and no slowness. */
        CHANNELING,
        /** A full insulated set: not locked onto, the strike lands a few blocks away; reduced shock damage. */
        INSULATED,
        /** Any worn or held conductive piece: locked first, drawn to, and retargeted by extra bolts. */
        CONDUCTIVE,
        /** Everything else: locked onto at their exact spot. */
        NORMAL
    }

    public static final double DIVERT_MIN = 3.0D;
    public static final double DIVERT_MAX = 8.0D;

    public static GearClass classify(boolean insulatedSet, boolean conductive, boolean channelingSet) {
        if (channelingSet) return GearClass.CHANNELING;
        if (insulatedSet) return GearClass.INSULATED;
        if (conductive) return GearClass.CONDUCTIVE;
        return GearClass.NORMAL;
    }

    /** Everyone but a full insulated set is locked onto. */
    public static boolean locksOn(GearClass gear) {
        return gear != GearClass.INSULATED;
    }

    /** A channeling set takes no damage and no slowness from any bolt. */
    public static boolean immuneToBolt(GearClass gear) {
        return gear == GearClass.CHANNELING;
    }

    /** Conductive and channeling players are retargeted by the extra bolts of a salvo. */
    public static boolean retargeted(GearClass gear) {
        return gear == GearClass.CHANNELING || gear == GearClass.CONDUCTIVE;
    }

    /** Relative chance to be picked as the primary target of a salvo. */
    public static int attractionWeight(GearClass gear) {
        return retargeted(gear) ? 4 : 1;
    }

    /** Order within a salvo: conductive and channeling players first, then the rest, insulated last. */
    public static int priority(GearClass gear) {
        if (retargeted(gear)) return 0;
        return gear == GearClass.NORMAL ? 1 : 2;
    }

    /** Distance an insulated player's strike is diverted: 3 to 8 blocks. */
    public static double divertDistance(Random rand) {
        return DIVERT_MIN + rand.nextDouble() * (DIVERT_MAX - DIVERT_MIN);
    }

    public static double shockDamage(boolean insulatedSet, double normal, double insulated) {
        return insulatedSet ? insulated : normal;
    }
}
