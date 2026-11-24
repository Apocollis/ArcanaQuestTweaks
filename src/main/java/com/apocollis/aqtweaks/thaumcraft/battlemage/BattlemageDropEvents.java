package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Routes {@code LivingDropsEvent} for the four battlemage entities to {@link BattlemageDrops}. Recognised by registry
 * id before any Thaumcraft class loads, like {@link BattlemageHurtEvents}.
 */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class BattlemageDropEvents {

    private BattlemageDropEvents() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living == null || living.world == null || living.world.isRemote) {
            return;
        }
        ResourceLocation key = EntityList.getKey(living);
        if (key == null || !EscortRelease.isBattlemagePath(key.getNamespace(), key.getPath())) {
            return;
        }
        BattlemageHurtRouter.routeDrops(living, event);
    }
}
