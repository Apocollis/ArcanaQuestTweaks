package com.apocollis.aqtweaks.additions;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

public final class NamelessBossRegistry {
    public static final String NAMELESS_ONE_ID = "nameless_one";
    public static final String NAMELESS_VESSEL_ID = "nameless_vessel";

    // Entity IDs within aqtweaks:
    // 1 = arcane_rift, 2 = deep_dwarf, 3 = bed_seat, 4 = reserved (cultist_battlemage),
    // 5 = nameless_one (Stage 2), 6 = nameless_vessel (Stage 1).
    public static final int ENTITY_ID_NAMELESS_ONE = 5;
    public static final int ENTITY_ID_NAMELESS_VESSEL = 6;

    // Spawn egg colors:
    // Stage 1 (Vessel): deep void slate primary (0x161923) + eldritch brass secondary (0xC9983E)
    // Stage 2 (True Form): abyssal chitin primary (0x181D24) + radiant void-gold secondary (0xD9DF52)
    public static final int EGG_VESSEL_PRIMARY = 0x161923;
    public static final int EGG_VESSEL_SECONDARY = 0xC9983E;
    public static final int EGG_ONE_PRIMARY = 0x181D24;
    public static final int EGG_ONE_SECONDARY = 0xD9DF52;

    private NamelessBossRegistry() {
    }

    public static void register(RegistryEvent.Register<EntityEntry> event) {
        if (ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.enable) {
            EntityEntry namelessOne = EntityEntryBuilder.create()
                    .entity(EntityNamelessOne.class)
                    .id(new ResourceLocation(ArcanaQuestTweaks.MODID, NAMELESS_ONE_ID), ENTITY_ID_NAMELESS_ONE)
                    .name(NAMELESS_ONE_ID)
                    .tracker(96, 1, true)
                    .egg(EGG_ONE_PRIMARY, EGG_ONE_SECONDARY)
                    .build();
            event.getRegistry().register(namelessOne);
        }

        if (ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessVessel.enable) {
            EntityEntry namelessVessel = EntityEntryBuilder.create()
                    .entity(EntityNamelessVessel.class)
                    .id(new ResourceLocation(ArcanaQuestTweaks.MODID, NAMELESS_VESSEL_ID), ENTITY_ID_NAMELESS_VESSEL)
                    .name(NAMELESS_VESSEL_ID)
                    .tracker(64, 1, true)
                    .egg(EGG_VESSEL_PRIMARY, EGG_VESSEL_SECONDARY)
                    .build();
            event.getRegistry().register(namelessVessel);
        }
    }
}
