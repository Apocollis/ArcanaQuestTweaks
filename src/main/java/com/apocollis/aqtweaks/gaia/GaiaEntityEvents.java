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
        event.getRegistry().register(net.minecraftforge.fml.common.registry.EntityEntryBuilder.create()
                .entity(com.apocollis.aqtweaks.somnia.EntityBedSeat.class)
                .id(new net.minecraft.util.ResourceLocation(ArcanaQuestTweaks.MODID, "bed_seat"), 3)
                .name("bed_seat")
                .tracker(256, 20, false)
                .build());

        if (!Loader.isModLoaded("grimoireofgaia") || !GrimoireOfGaiaConfig.enableDeepDwarf) {
            return;
        }
        GaiaDeepDwarfRegistry.register(event);
    }
}
