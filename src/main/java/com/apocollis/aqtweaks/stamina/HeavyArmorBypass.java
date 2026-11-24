package com.apocollis.aqtweaks.stamina;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Direct melee hits by a player ignore a share of the target's armor reduction, by weapon category (heavy 25% by
 * default). The armor-only loss is measured in {@code LivingHurtEvent} (final amount, so DSS's combo bonus counts),
 * and the share is added back in {@code LivingDamageEvent}, after armor. Protection enchantments, potions and
 * resistance are untouched. Special armor that bypasses the vanilla formula is only approximated.
 */
public final class HeavyArmorBypass {

    private EntityLivingBase pendingTarget;
    private float pendingLoss;
    private float pendingShare;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurt(LivingHurtEvent event) {
        pendingTarget = null;
        EntityLivingBase target = event.getEntityLiving();
        if (target.world.isRemote) return;
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof EntityPlayer) || source.getImmediateSource() != source.getTrueSource()) {
            return;
        }
        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        if (player instanceof FakePlayer || source.isUnblockable() || source.isDamageAbsolute()
                || !"player".equals(source.getDamageType())) {
            return;
        }
        float share = DssWeaponBalance.armorBypassShare(player);
        if (share <= 0.0F) return;
        float amount = event.getAmount();
        float armor = target.getTotalArmorValue();
        float toughness = (float) target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
        float loss = amount - CombatRules.getDamageAfterAbsorb(amount, armor, toughness);
        if (loss <= 0.0F) return;
        pendingTarget = target;
        pendingLoss = loss;
        pendingShare = share;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamage(LivingDamageEvent event) {
        EntityLivingBase target = pendingTarget;
        pendingTarget = null;
        if (target == null || event.getEntityLiving() != target) return;
        event.setAmount(event.getAmount() + pendingShare * pendingLoss);
    }
}
