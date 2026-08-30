package com.apocollis.aqtweaks.gaia;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

public final class GaiaPierce {

    private GaiaPierce() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.GrimoireOfGaiaConfig.disablePiercingDamage;
    }

    public static void onMeleeInstantDamage(EntityLivingBase victim, PotionEffect effect) {
        if (!enabled() || !(victim instanceof EntityPlayer)) {
            victim.addPotionEffect(effect);
            return;
        }
        if (effect != null && effect.getPotion() == MobEffects.INSTANT_DAMAGE) {
            return;
        }
        victim.addPotionEffect(effect);
    }

    public static void onArcherInstantTip(EntityTippedArrow arrow, PotionEffect effect) {
        if (!enabled()) {
            arrow.addEffect(effect);
            return;
        }
        if (effect != null && effect.getPotion() == MobEffects.INSTANT_DAMAGE) {
            return;
        }
        arrow.addEffect(effect);
    }

    public static boolean onMagicBoltHit(Entity projectile, Entity victim, DamageSource source, float amount) {
        if (!enabled() || source != DamageSource.MAGIC || !(victim instanceof EntityPlayer)) {
            return victim.attackEntityFrom(source, amount);
        }
        Entity shooter = shooterOf(projectile);
        float dmg = scaled(shooter, GaiaDamageConfig.get().spellMultiplier, amount);
        return victim.attackEntityFrom(DamageSource.causeIndirectMagicDamage(projectile, shooter), dmg);
    }

    public static boolean onBombHit(Entity bomb, Entity victim, DamageSource source, float amount) {
        if (!enabled() || !(victim instanceof EntityPlayer)) {
            return victim.attackEntityFrom(source, amount);
        }
        if (source == DamageSource.MAGIC) {
            Entity thrower = shooterOf(bomb);
            float dmg = scaled(thrower, GaiaDamageConfig.get().bombMultiplier, amount);
            return victim.attackEntityFrom(new GaiaDamageSources.Bomb(bomb, thrower), dmg);
        }
        return false;
    }

    private static Entity shooterOf(Entity projectile) {
        if (projectile instanceof EntityFireball) {
            return ((EntityFireball) projectile).shootingEntity;
        }
        if (projectile instanceof net.minecraft.entity.projectile.EntityThrowable) {
            return ((net.minecraft.entity.projectile.EntityThrowable) projectile).getThrower();
        }
        return null;
    }

    private static float scaled(Entity shooter, float multiplier, float fallback) {
        if (!(shooter instanceof EntityLivingBase)) {
            return fallback;
        }
        IAttributeInstance inst = ((EntityLivingBase) shooter)
                .getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (inst == null) {
            return fallback;
        }
        return (float) (inst.getAttributeValue() * multiplier);
    }
}
