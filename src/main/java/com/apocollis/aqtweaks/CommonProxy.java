package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.stamina.PacketSyncClimbingInput;

import com.apocollis.aqtweaks.thaumcraft.BewitchmentRegistryHandler;

import com.apocollis.aqtweaks.stamina.PacketLedgeClimb;

import com.apocollis.aqtweaks.stamina.PacketSyncGrappleInput;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftModule;

import com.apocollis.aqtweaks.stamina.StaminaModule;

import com.apocollis.aqtweaks.comfort.ComfortConfigLoader;

import com.apocollis.aqtweaks.gaia.GaiaDamageConfig;
import com.apocollis.aqtweaks.gaia.GaiaDamageHandler;

import com.apocollis.aqtweaks.comfort.ComfortSystemHandler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketSyncClimbingInput.Handler.class, PacketSyncClimbingInput.class, 0, Side.SERVER);
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketLedgeClimb.Handler.class, PacketLedgeClimb.class, 1, Side.SERVER);
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketSyncGrappleInput.Handler.class, PacketSyncGrappleInput.class, 2, Side.SERVER);
        ComfortConfigLoader.load(event.getModConfigurationDirectory());
        GaiaDamageConfig.load(event.getModConfigurationDirectory());
        com.apocollis.aqtweaks.portal.PortalModule.preInit();
        net.minecraft.world.gen.structure.MapGenStructureIO.registerStructureComponent(
                com.apocollis.aqtweaks.rtg.VillagePieceVillagePlate.class, "AQTVillagePlate");
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new StaminaModule());
        MinecraftForge.EVENT_BUS.register(new GaiaDamageHandler());

        if (net.minecraftforge.fml.common.Loader.isModLoaded("thaumcraft")) {
            MinecraftForge.EVENT_BUS.register(new ThaumcraftModule());
        }

        if (net.minecraftforge.fml.common.Loader.isModLoaded("bewitchment")) {
            MinecraftForge.EVENT_BUS.register(new BewitchmentRegistryHandler());
        }

        MinecraftForge.EVENT_BUS.register(new ComfortSystemHandler());
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.rtg.VillageLandHelper.Events());

        if (net.minecraftforge.fml.common.Loader.isModLoaded("astralsorcery")) {
            com.apocollis.aqtweaks.rtg.VillageAstralSmallShrineHandler.register();
        }
    }

    public void postInit(FMLPostInitializationEvent event) {}
}
