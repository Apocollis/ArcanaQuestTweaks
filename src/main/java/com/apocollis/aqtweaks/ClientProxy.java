package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.client.BaublesMenuClient;
import com.apocollis.aqtweaks.client.ClientModule;
import com.apocollis.aqtweaks.client.GuiMouseGrab;
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
        RenderingRegistry.registerEntityRenderingHandler(
                com.apocollis.aqtweaks.somnia.EntityBedSeat.class,
                com.apocollis.aqtweaks.somnia.client.RenderBedSeat::new);
        if (net.minecraftforge.fml.common.Loader.isModLoaded("grimoireofgaia")
                && com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.enableDeepDwarf) {
            com.apocollis.aqtweaks.gaia.client.GaiaDeepDwarfClient.preInit();
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new StaminaModuleClient());
        if (net.minecraftforge.fml.common.Loader.isModLoaded("dynamicswordskills")) {
            DssSkillsGuiClient.register();
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("baubles")) {
            BaublesMenuClient.register();
        }
        MinecraftForge.EVENT_BUS.register(new DepthsFogHandler());
        if (net.minecraftforge.fml.common.Loader.isModLoaded("atum")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.atum.AtumFogHandler());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.atum.AtumOutdoorDustEmitter());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.atum.AtumSoundFilter());
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("theaurorian")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.aurorian.AurorianSnowEmitter());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.aurorian.AurorianSoundFilter());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.aurorian.AurorianFrostOverlay());
        }
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.HazardDebugHud());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.nether.SpoutEffects());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.nether.SpoutTremor());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.nether.AshCloudRenderer());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.nether.AshFogHandler());
        if (net.minecraftforge.fml.common.Loader.isModLoaded("aether_legacy")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.aether.AetherSquallClient());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.aether.AetherBoltEffects());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.aether.AetherStormWarning());
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("metallurgy")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.paragon.ParagonTooltipHandler());
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("thaumcraft")
                && net.minecraftforge.fml.common.Loader.isModLoaded("botania")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.manarepair.ManaRepairTooltipHandler());
        }
        if (net.minecraftforge.fml.common.Loader.isModLoaded("twilightforest")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.twilightforest.LeyRuptureClient());
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.client.twilightforest.LeyRuptureVignette());
        }
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.rarity.client.RarityTooltipHandler());
        MinecraftForge.EVENT_BUS.register(new ClientModule());
        MinecraftForge.EVENT_BUS.register(new GuiMouseGrab());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.reskillable.client.ProspectorOutline());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.comfort.ComfortHud());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.comfort.ComfortTooltips());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.comfort.ComfortToggleButton());
    }
}
