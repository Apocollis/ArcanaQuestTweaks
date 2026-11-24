package com.apocollis.aqtweaks.starter.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.starter.StarterItemRegistry;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Item model for Tabula Rasa. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ArcanaQuestTweaks.MODID)
public final class StarterClientEvents {

    private StarterClientEvents() {}

    @SubscribeEvent
    public static void onModels(ModelRegistryEvent event) {
        if (StarterItemRegistry.TABULA_RASA == null) return;
        ModelLoader.setCustomModelResourceLocation(StarterItemRegistry.TABULA_RASA, 0,
                new ModelResourceLocation(StarterItemRegistry.TABULA_RASA.getRegistryName(), "inventory"));
    }
}
