package com.apocollis.aqtweaks.potion;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;

/** Public entry points for the Stun effect, so other systems (and mob AI) can apply and read it. */
public final class Stun {
    private Stun() {}

    public static boolean isStunned(EntityLivingBase entity) {
        return entity != null && entity.isPotionActive(PotionStun.STUN);
    }

    /**
     * Stun for {@code ticks}. Creative and spectator players are skipped. Ends any use in progress (a bow draw,
     * a meal, a raised shield) and, for mobs, stops the current path. Server side only.
     */
    public static void apply(EntityLivingBase entity, int ticks) {
        if (entity == null || ticks <= 0 || entity.world.isRemote) return;
        if (entity instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) entity;
            if (p.isCreative() || p.isSpectator()) return;
        }
        if (entity instanceof IStunResistant) {
            ticks = ((IStunResistant) entity).adjustStun(ticks);
            if (ticks <= 0) return;
        }
        PotionEffect current = entity.getActivePotionEffect(PotionStun.STUN);
        if (current == null || current.getDuration() < ticks) {
            entity.addPotionEffect(new PotionEffect(PotionStun.STUN, ticks, 0, false, true));
        }
        entity.resetActiveHand();
        if (entity instanceof EntityLiving) {
            ((EntityLiving) entity).getNavigator().clearPath();
        }
    }
}
