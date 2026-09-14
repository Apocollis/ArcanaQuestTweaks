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
        // Forge has already read the cfg files by now; pin the keys that are not tunable.
        com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.normalizePinned();
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketSyncClimbingInput.Handler.class, PacketSyncClimbingInput.class, 0, Side.SERVER);
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketLedgeClimb.Handler.class, PacketLedgeClimb.class, 1, Side.SERVER);
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketSyncGrappleInput.Handler.class, PacketSyncGrappleInput.class, 2, Side.SERVER);
        ComfortConfigLoader.load(event.getModConfigurationDirectory());
        GaiaDamageConfig.load(event.getModConfigurationDirectory());
        com.apocollis.aqtweaks.spawning.SpawnTypeLists.load(event.getModConfigurationDirectory());
        com.apocollis.aqtweaks.spawning.SpawnParties.load(event.getModConfigurationDirectory());
        com.apocollis.aqtweaks.spawning.SpawnGroupCounts.load(event.getModConfigurationDirectory());
        com.apocollis.aqtweaks.portal.PortalModule.preInit();
        if (net.minecraftforge.fml.common.Loader.isModLoaded("reskillable")) {
            com.apocollis.aqtweaks.reskillable.ReskillablePerkRegistry.preInit();
        }
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

        if (net.minecraftforge.fml.common.Loader.isModLoaded("reskillable")) {
            MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.reskillable.ReskillableModule());
        }

        if (net.minecraftforge.fml.common.Loader.isModLoaded("somnia")) {
            com.apocollis.aqtweaks.somnia.SomniaSleepHandler.init();
        }
    }

    public void postInit(FMLPostInitializationEvent event) {
        // After InControl so PotentialSpawns last-per-class runs after it appends group-count rows.
        MinecraftForge.EVENT_BUS.register(new com.apocollis.aqtweaks.spawning.SpawnLayerFilter());
        // Every optional-mod and vanilla handle has had its chance to resolve by now. Say which
        // ones did not, so a mapping break is a log line instead of a module that quietly no-ops.
        com.apocollis.aqtweaks.util.Reflect.auditUnresolved();
    }
}
