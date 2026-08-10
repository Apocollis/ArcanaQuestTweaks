package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.bewitchment.api.registry.Ritual;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class BewitchmentRegistryHandler {

    @SubscribeEvent
    public void registerRituals(RegistryEvent.Register<Ritual> event) {
        if (!Loader.isModLoaded("bewitchment") || !Loader.isModLoaded("thaumcraft")) return;

        IForgeRegistry<Ritual> registry = event.getRegistry();

        for (String entry : ArcanaQuestTweaksConfig.bewitchmentModule.ritualWarpList) {
            String[] parts = entry.split("=");
            if (parts.length != 2) continue;

            ResourceLocation id = new ResourceLocation(parts[0].trim());
            Ritual original = registry.getValue(id);
            if (original != null) {
                String[] warpParts = parts[1].split(",");
                if (warpParts.length < 2) continue;

                try {
                    int normal = Integer.parseInt(warpParts[0].trim());
                    int temp = Integer.parseInt(warpParts[1].trim());
                    int permanent = 0;
                    if (warpParts.length >= 3) {
                        permanent = Integer.parseInt(warpParts[2].trim());
                    }

                    WarpRitualWrapper wrapper = new WarpRitualWrapper(original, normal, temp, permanent);
                    registry.register(wrapper);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
