package com.apocollis.aqtweaks;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketSyncClimbingInput.Handler.class, PacketSyncClimbingInput.class, 0, Side.SERVER);
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketLedgeClimb.Handler.class, PacketLedgeClimb.class, 1, Side.SERVER);
        ComfortConfigLoader.load(event.getModConfigurationDirectory());
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new StaminaModule());
        MinecraftForge.EVENT_BUS.register(new GrimoireOfGaiaModule());

        if (net.minecraftforge.fml.common.Loader.isModLoaded("thaumcraft")) {
            MinecraftForge.EVENT_BUS.register(new ThaumcraftModule());
        }

        if (net.minecraftforge.fml.common.Loader.isModLoaded("bewitchment")) {
            MinecraftForge.EVENT_BUS.register(new BewitchmentRegistryHandler());
        }

        MinecraftForge.EVENT_BUS.register(new ComfortSystemHandler());
    }

    public void postInit(FMLPostInitializationEvent event) {}
}
