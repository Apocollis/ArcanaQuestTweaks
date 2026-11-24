package com.apocollis.aqtweaks.starter;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Registers Tabula Rasa when the starter module is active (Reskillable and CustomStartingGear present). */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class StarterItemRegistry {

    @GameRegistry.ObjectHolder(ArcanaQuestTweaks.MODID + ":" + ItemTabulaRasa.ID)
    public static final Item TABULA_RASA = null;

    private StarterItemRegistry() {}

    public static boolean enabled() {
        return Loader.isModLoaded("reskillable") && Loader.isModLoaded("customstartinggear");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) return;
        event.getRegistry().register(new ItemTabulaRasa());
    }
}
