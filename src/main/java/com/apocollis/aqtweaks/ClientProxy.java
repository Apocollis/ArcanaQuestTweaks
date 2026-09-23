package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.client.ClientModule;
import com.apocollis.aqtweaks.depths.DepthsFogHandler;
import com.apocollis.aqtweaks.portal.EntityArcaneRift;
import com.apocollis.aqtweaks.portal.client.RenderArcaneRift;
import com.apocollis.aqtweaks.stamina.DssSkillsGuiClient;
import com.apocollis.aqtweaks.stamina.StaminaModuleClient;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityArcaneRift.class, RenderArcaneRift::new);
        if (net.minecraftforge.fml.common.Loader.isModLoaded("grimoireofgaia")
                && com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.enableDeepDwarf) {
            com.apocollis.aqtweaks.gaia.client.GaiaDeepDwarfClient.preInit();
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new StaminaModuleClient());
        DssSkillsGuiClient.register();
        MinecraftForge.EVENT_BUS.register(new DepthsFogHandler());
        MinecraftForge.EVENT_BUS.register(new ClientModule());
    }
}
