package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** Holds the cast to the Thaumcraft-based entity class, so {@link BattlemageHurtEvents} never loads it for other mobs. */
final class BattlemageHurtRouter {

    private BattlemageHurtRouter() {}

    static void routeDrops(EntityLivingBase living, net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (living instanceof EntityCultistBattlemage) {
            BattlemageDrops.onDrops((EntityCultistBattlemage) living, event);
        }
    }

    /** A hit dealt by a battlemage: the optional per-hit damage cap (plan 3.13 item 8). */
    static void routeAttack(EntityLivingBase attacker, LivingHurtEvent event) {
        if (attacker instanceof EntityCultistBattlemage) {
            double frac = com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig.castDamageCapFraction;
            if (frac > 0.0D) {
                float cap = (float) (event.getEntityLiving().getMaxHealth() * frac);
                if (event.getAmount() > cap) {
                    event.setAmount(cap);
                }
            }
        }
    }

    static void route(EntityLivingBase living, LivingHurtEvent event) {
        if (living instanceof EntityCultistBattlemage) {
            BattlemageShield.onHurt((EntityCultistBattlemage) living, event);
        }
    }
}
