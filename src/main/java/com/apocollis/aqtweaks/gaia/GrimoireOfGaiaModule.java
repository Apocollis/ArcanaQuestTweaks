package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grimoire of Gaia applies piercing/magic damage through:
 * 1. Melee attacks (instantly applies INSTANT_DAMAGE potion on hit on the same tick,
 *    firing a separate MAGIC damage event with no entity references).
 * 2. Projectile impacts (fires MAGIC damage with no entity reference).
 *
 * Solution:
 * We use tick correlation to detect these cases on the same world tick.
 * - When a Gaia mob physically hits a player, we record (playerUUID -> GaiaDamageInfo).
 * - When a Gaia projectile impacts a player, we record (playerUUID -> GaiaDamageInfo).
 * - When magic/absolute/unblockable damage hits a player on that exact same tick,
 *   we cancel it and re-trigger it using custom Melee/Projectile damage sources.
 * - This custom DamageSource allows physical armor, general Protection, Projectile Protection,
 *   and Magic Protection (e.g. from Bewitchment) to all apply natively.
 * - We temporarily clear player.hurtResistantTime so the re-triggered attack isn't
 *   blocked by the player's invulnerability frames (i-frames) from the physical hit.
 */
public class GrimoireOfGaiaModule {

    private static class GaiaDamageInfo {
        final long tick;
        final net.minecraft.entity.Entity attacker;
        final net.minecraft.entity.Entity projectile;

        GaiaDamageInfo(long tick, net.minecraft.entity.Entity attacker, net.minecraft.entity.Entity projectile) {
            this.tick = tick;
            this.attacker = attacker;
            this.projectile = projectile;
        }
    }

    private final Map<UUID, GaiaDamageInfo> gaiaDamageTracker = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.disablePiercingDamage) return;
        Entity hit = Reflect.getEntityHit(event.getRayTraceResult());
        if (!(hit instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) hit;
        net.minecraft.entity.Entity projectile = event.getEntity();
        if (projectile != null && projectile.getClass().getName().startsWith("gaia.")) {
            net.minecraft.entity.Entity shooter = Reflect.getShootingEntity(projectile);
            long tick = Reflect.getWorld(player) != null ? Reflect.getWorld(player).getTotalWorldTime() : 0L;
            gaiaDamageTracker.put(Reflect.getUniqueID(player), new GaiaDamageInfo(tick, shooter, projectile));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingHurt(LivingHurtEvent event) {
        if (!ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.disablePiercingDamage) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        DamageSource source = event.getSource();
        long tick = Reflect.getWorld(player) != null ? Reflect.getWorld(player).getTotalWorldTime() : 0L;

        // Prevent infinite recursion when our custom re-routed damage event fires
        if (source instanceof GaiaDamageSources.Melee || source instanceof GaiaDamageSources.Projectile) {
            return;
        }

        // 1. If this is a physical attack from a Gaia mob, record it
        Entity trueSource = Reflect.getTrueSource(source);
        if (trueSource != null && trueSource.getClass().getName().startsWith("gaia.")) {
            gaiaDamageTracker.put(Reflect.getUniqueID(player), new GaiaDamageInfo(tick, trueSource, null));
            return;
        }

        // 2. If this is magic/piercing damage and a Gaia attack occurred on this tick, re-route it
        if (Reflect.isUnblockable(source) || Reflect.isDamageAbsolute(source) || Reflect.isMagicDamage(source)) {
            GaiaDamageInfo entry = gaiaDamageTracker.get(Reflect.getUniqueID(player));
            if (entry != null && entry.tick == tick) {
                // Cancel the original magic damage event
                event.setCanceled(true);

                // Construct custom damage source that preserves isMagicDamage() but does not bypass armor
                DamageSource customSource;
                if (entry.projectile != null) {
                    customSource = new GaiaDamageSources.Projectile(entry.projectile, entry.attacker);
                } else {
                    customSource = new GaiaDamageSources.Melee(entry.attacker);
                }

                // Temporarily bypass invulnerability frames (i-frames) so damage is not blocked by the physical hit
                int tempHurtResistant = Reflect.getHurtResistantTime(player);
                Reflect.setHurtResistantTime(player, 0);

                Reflect.attackEntityFrom(player, customSource, event.getAmount());

                Reflect.setHurtResistantTime(player, tempHurtResistant);
            }
        }
    }
}
