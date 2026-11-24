package com.apocollis.aqtweaks.titles;

import net.minecraftforge.fml.common.Loader;

/**
 * Entry point for the structure title packet on the client. It names no Traveler's Titles type, so it loads
 * without that mod; the hook class that does is only touched once the mod is known to be present.
 */
public final class StructureTitleClient {

    private StructureTitleClient() {}

    public static void apply(String key, String title, String subtitle, boolean enter) {
        if (!enter || !Loader.isModLoaded("travelerstitles")) {
            return;
        }
        TravelersTitlesStructureHook.show(key, title, subtitle);
    }
}
