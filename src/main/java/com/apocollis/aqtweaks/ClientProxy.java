package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.stamina.StaminaModuleClient;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new StaminaModuleClient());
    }
}
