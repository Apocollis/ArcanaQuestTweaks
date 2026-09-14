package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

/** No Gaia imports — safe if the parent jar is absent. */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class GaiaEntityEvents {

    private GaiaEntityEvents() {}

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!Loader.isModLoaded("grimoireofgaia") || !GrimoireOfGaiaConfig.enableDeepDwarf) {
            return;
        }
        GaiaDeepDwarfRegistry.register(event);
    }
}
