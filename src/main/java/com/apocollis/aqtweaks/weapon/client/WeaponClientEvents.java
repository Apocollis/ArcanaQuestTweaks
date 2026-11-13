package com.apocollis.aqtweaks.weapon.client;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.weapon.WeaponModule;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Binds the Infinity Blade's inventory model. Client only. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ArcanaQuestTweaks.MODID)
public final class WeaponClientEvents {
    private WeaponClientEvents() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(WeaponModule.INFINITY_BLADE, 0,
                new ModelResourceLocation(WeaponModule.INFINITY_BLADE.getRegistryName(), "inventory"));
    }
}
