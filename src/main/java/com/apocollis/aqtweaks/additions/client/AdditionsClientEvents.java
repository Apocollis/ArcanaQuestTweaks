package com.apocollis.aqtweaks.additions.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.additions.AdditionsModule;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID, value = Side.CLIENT)
public final class AdditionsClientEvents {
    private AdditionsClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (AdditionsModule.INFINITY_BLADE != null) {
            ModelLoader.setCustomModelResourceLocation(
                    AdditionsModule.INFINITY_BLADE,
                    0,
                    new ModelResourceLocation(AdditionsModule.INFINITY_BLADE_REGISTRY_NAME, "inventory")
            );
        }
    }
}
