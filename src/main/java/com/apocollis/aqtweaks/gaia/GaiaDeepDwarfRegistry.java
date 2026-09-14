package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

public final class GaiaDeepDwarfRegistry {

    public static final ResourceLocation ID = new ResourceLocation(ArcanaQuestTweaks.MODID, "deep_dwarf");
    public static final String ID_STRING = "aqtweaks:deep_dwarf";

    private GaiaDeepDwarfRegistry() {}

    public static void register(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityDeepDwarf.class)
                .id(ID, 2)
                .name("deep_dwarf")
                .tracker(64, 3, true)
                .egg(0x6B4A9A, 0x2A1840)
                .build());
    }
}
