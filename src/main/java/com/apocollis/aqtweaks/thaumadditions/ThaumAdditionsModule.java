package com.apocollis.aqtweaks.thaumadditions;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.thaumadditions.foci.FocusEffectAegis;
import com.apocollis.aqtweaks.thaumadditions.foci.FocusEffectShock;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.casters.FocusEngine;

/** Thaumcraft Additions: new Thaumcraft content owned by this pack. Does nothing without Thaumcraft. */
public final class ThaumAdditionsModule {
    private ThaumAdditionsModule() {}

    /** Call from init, after Thaumcraft's preInit and before its postInit reads the research files. */
    public static void init() {
        if (!Loader.isModLoaded("thaumcraft")) return;
        boolean shock = ArcanaQuestTweaksConfig.ThaumAdditionsConfig.enableShockFocus;
        boolean aegis = ArcanaQuestTweaksConfig.ThaumAdditionsConfig.enableAegisFocus;
        if (shock) {
            FocusEngine.registerElement(FocusEffectShock.class,
                    new ResourceLocation("aqtweaks", "textures/foci/shock.png"), FocusEffectShock.COLOR);
        }
        if (aegis) {
            FocusEngine.registerElement(FocusEffectAegis.class,
                    new ResourceLocation("aqtweaks", "textures/foci/aegis.png"), FocusEffectAegis.COLOR);
            AegisChannelHandler.register();
        }
        if (shock || aegis) {
            ThaumcraftApi.registerResearchLocation(new ResourceLocation("aqtweaks", "research/thaumadditions"));
        }
        if (com.apocollis.aqtweaks.starter.StarterItemRegistry.TABULA_RASA != null
                && com.apocollis.aqtweaks.starter.StarterThaumcraftHelper.registerTabulaRasaInfusion()) {
            ThaumcraftApi.registerResearchLocation(new ResourceLocation("aqtweaks", "research/tabula_rasa"));
        }
    }
}
