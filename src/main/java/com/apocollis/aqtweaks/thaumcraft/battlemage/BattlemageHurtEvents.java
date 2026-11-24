package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Routes {@code LivingHurtEvent} for the four battlemage entities to {@link BattlemageShield#onHurt} (plan 3.7). The
 * entity is recognised by registry id before any Thaumcraft class is touched, so this class loads safely without the parent.
 */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class BattlemageHurtEvents {

    private BattlemageHurtEvents() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living == null || living.world == null || living.world.isRemote) {
            return;
        }
        net.minecraft.entity.Entity source = event.getSource().getTrueSource();
        if (source instanceof EntityLivingBase && source != living) {
            ResourceLocation attackerKey = EntityList.getKey(source);
            if (attackerKey != null && EscortRelease.isBattlemagePath(attackerKey.getNamespace(), attackerKey.getPath())) {
                BattlemageHurtRouter.routeAttack((EntityLivingBase) source, event);
            }
        }
        ResourceLocation key = EntityList.getKey(living);
        if (key == null || !EscortRelease.isBattlemagePath(key.getNamespace(), key.getPath())) {
            return;
        }
        BattlemageHurtRouter.route(living, event);
    }
}
