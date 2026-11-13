package com.apocollis.aqtweaks.paragon;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;

/** Wires the Paragon traits: each registers only when its metal's mod is present, and all need Metallurgy's Tartarite. */
public final class ParagonModule {
    private ParagonModule() {}

    /** Before recipes load, because the recipe files name the traits. */
    public static void preInit() {
        if (!Loader.isModLoaded("metallurgy")) return;
        if (Loader.isModLoaded("theaurorian")) ParagonRegistry.register(new AurorianSteelParagonTrait());
        if (Loader.isModLoaded("twilightforest")) {
            ParagonRegistry.register(new FieryParagonTrait());
            ParagonRegistry.register(new KnightmetalParagonTrait());
        }
        if (Loader.isModLoaded("thebetweenlands")) ParagonRegistry.register(new OctineParagonTrait());
        if (Loader.isModLoaded("aether_legacy")) {
            ParagonRegistry.register(new ZaniteParagonTrait());
            ParagonRegistry.register(new GravititeParagonTrait());
        }
    }

    public static void init() {
        if (Loader.isModLoaded("metallurgy")) MinecraftForge.EVENT_BUS.register(new ParagonEventHandler());
    }
}
