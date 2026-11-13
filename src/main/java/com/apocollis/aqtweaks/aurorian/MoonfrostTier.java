package com.apocollis.aqtweaks.aurorian;

/** Gear tier against the Moonfrost storm. No Minecraft types. */
public enum MoonfrostTier {
    /** Any armor slot empty. */
    EXPOSED,
    /** Four slots of ordinary armor. */
    ARMORED,
    /** Cold Resistance effect active, or four slots on the proper-gear whitelist. */
    PROPER;

    public static MoonfrostTier classify(boolean allSlotsArmor, boolean hasGearEffect, boolean allSlotsWhitelisted) {
        if (!allSlotsArmor) return EXPOSED;
        if (hasGearEffect || allSlotsWhitelisted) return PROPER;
        return ARMORED;
    }
}
