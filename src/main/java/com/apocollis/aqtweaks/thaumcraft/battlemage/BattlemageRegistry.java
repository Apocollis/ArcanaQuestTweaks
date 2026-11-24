package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

/**
 * Entity registration for the Crimson Battlemage family (plan 18.8): the Battlemage, the Master Battlemage and the two
 * variants. aqtweaks entity ids in use elsewhere: 1 arcane_rift, 2 deep_dwarf, 3 bed_seat, 5 nameless_one,
 * 6 nameless_vessel (additions). Id 10 is reserved for the cosmetic escort portal (plan 18.7). Entity ids must be unique per mod.
 */
public final class BattlemageRegistry {

    public static final ResourceLocation ID = new ResourceLocation(ArcanaQuestTweaks.MODID, "cultist_battlemage");
    public static final ResourceLocation MASTER_ID = new ResourceLocation(ArcanaQuestTweaks.MODID, "cultist_master_battlemage");
    public static final ResourceLocation SWARM_ID = new ResourceLocation(ArcanaQuestTweaks.MODID, "cultist_swarm_mage");
    public static final ResourceLocation BOMBARDIER_ID = new ResourceLocation(ArcanaQuestTweaks.MODID, "cultist_bombardier_mage");

    public static final int ENTITY_ID = 4;
    public static final int MASTER_ENTITY_ID = 7;
    public static final int SWARM_ENTITY_ID = 8;
    public static final int BOMBARDIER_ENTITY_ID = 9;
    /** The cosmetic escort portal entity (plan 18.7). */
    public static final int ESCORT_PORTAL_ENTITY_ID = 10;
    public static final ResourceLocation ESCORT_PORTAL_ID = new ResourceLocation(ArcanaQuestTweaks.MODID, "escort_portal");

    private BattlemageRegistry() {}

    public static void register(RegistryEvent.Register<EntityEntry> event) {
        add(event, EntityCultistBattlemage.class, ID, ENTITY_ID, "cultist_battlemage", 0x6E0F14, 0xC9983E);
        add(event, EntityCultistMasterBattlemage.class, MASTER_ID, MASTER_ENTITY_ID, "cultist_master_battlemage", 0x4A0A12, 0xE3B95A);
        add(event, EntityCultistSwarmMage.class, SWARM_ID, SWARM_ENTITY_ID, "cultist_swarm_mage", 0x6E0F14, 0x7A3FA0);
        add(event, EntityCultistBombardierMage.class, BOMBARDIER_ID, BOMBARDIER_ENTITY_ID, "cultist_bombardier_mage", 0x4A0A12, 0xD9622B);
        // The cosmetic portal the Swarm Mage's escort drops out of: no spawn egg, never saved (plan 18.7).
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityEscortPortal.class)
                .id(ESCORT_PORTAL_ID, ESCORT_PORTAL_ENTITY_ID)
                .name("escort_portal")
                .tracker(80, 20, false)
                .build());
    }

    private static void add(RegistryEvent.Register<EntityEntry> event, Class<? extends Entity> type, ResourceLocation id,
            int numericId, String name, int eggPrimary, int eggSecondary) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(type)
                .id(id, numericId)
                .name(name)
                .tracker(80, 3, true)
                .egg(eggPrimary, eggSecondary)
                .build());
    }
}
