package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.additions.EntityNamelessOne;
import com.apocollis.aqtweaks.additions.EntityNamelessVessel;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NamelessBossClient {
    private NamelessBossClient() {
    }

    public static void preInit() {
        if (ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessVessel.enable) {
            RenderingRegistry.registerEntityRenderingHandler(EntityNamelessVessel.class, RenderNamelessVessel::new);
        }
        if (ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.enable) {
            RenderingRegistry.registerEntityRenderingHandler(EntityNamelessOne.class, RenderNamelessOne::new);
        }
    }
}
