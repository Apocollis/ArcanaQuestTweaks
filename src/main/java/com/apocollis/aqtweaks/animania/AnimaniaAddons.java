package com.apocollis.aqtweaks.animania;

import net.minecraftforge.fml.common.Loader;

/**
 * Which Animania addons are installed. The Farm addon ships inside its own jar but registers no mod
 * id of its own, so a class-presence probe is the only available signal. This is detection, not a
 * reflective call: once {@link #FARM} is true, callers use {@link AnimaniaFarmProducts} and
 * {@link AnimaniaFarmClocks} directly.
 */
public final class AnimaniaAddons {

    /** True when Animania and its Farm addon classes are present. */
    public static final boolean FARM = detectFarm();

    private AnimaniaAddons() {}

    private static boolean detectFarm() {
        if (!Loader.isModLoaded("animania")) return false;
        try {
            Class.forName("com.animania.addons.farm.common.handler.FarmAddonBlockHandler", false,
                    AnimaniaAddons.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
