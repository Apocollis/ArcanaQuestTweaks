package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

/** No Thaumcraft imports: safe when the parent is absent. */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class BattlemageEntityEvents {

    private BattlemageEntityEvents() {}

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!Loader.isModLoaded("thaumcraft") || !ArcanaQuestTweaksConfig.BattlemageConfig.enableBattlemage) {
            return;
        }
        BattlemageRegistry.register(event);
    }
}
