package com.apocollis.aqtweaks.portal.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.portal.PortalModule;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ArcanaQuestTweaks.MODID)
public final class PortalClientEvents {

    private PortalClientEvents() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        register(PortalModule.TEAR);
        register(PortalModule.WILD);
    }

    private static void register(Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
