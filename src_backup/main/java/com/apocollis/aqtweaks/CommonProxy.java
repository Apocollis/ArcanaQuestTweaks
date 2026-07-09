package com.apocollis.aqtweaks;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ArcanaQuestTweaks.NETWORK.registerMessage(PacketSyncClimbingInput.Handler.class, PacketSyncClimbingInput.class, 0, Side.SERVER);
    }

    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new StaminaModule());
    }

    public void postInit(FMLPostInitializationEvent event) {}
}
