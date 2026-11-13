package com.apocollis.aqtweaks.paragon;

import java.util.List;
import java.util.UUID;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.EnumSet;

/**
 * Zanite: the item grows stronger as it wears down. Mining speed and sword damage rise through five wear tiers, and each
 * armor piece adds armor points by its tier. The Aether's own steps are kept as shares of the item's durability.
 */
public class ZaniteParagonTrait extends ParagonTrait {
    private static final EntityEquipmentSlot[] ARMOR = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
    private static final UUID[] MODIFIERS = {
            UUID.fromString("5f1e2c10-7a41-4b0e-9a55-2f6c0a7d1001"),
            UUID.fromString("5f1e2c10-7a41-4b0e-9a55-2f6c0a7d1002"),
            UUID.fromString("5f1e2c10-7a41-4b0e-9a55-2f6c0a7d1003"),
            UUID.fromString("5f1e2c10-7a41-4b0e-9a55-2f6c0a7d1004")};

    public ZaniteParagonTrait() {
        super("zanite", "blockZanite", "aether_legacy", EnumSet.of(TartariteKind.SWORD, TartariteKind.PICKAXE,
                TartariteKind.AXE, TartariteKind.SHOVEL, TartariteKind.HELMET, TartariteKind.CHESTPLATE,
                TartariteKind.LEGGINGS, TartariteKind.BOOTS));
    }

    private static int tierOf(ItemStack stack) {
        return WearTier.tier(stack.getItemDamage(), stack.getMaxDamage());
    }

    @Override
    public boolean tracksWearer() {
        return true;
    }

    @Override
    public void onBreakSpeed(PlayerEvent.BreakSpeed event, EntityPlayer player, ItemStack stack, TartariteKind kind) {
        if (!kind.isTool()) return;
        float factor = WearTier.speedFactor(tierOf(stack));
        if (factor > 1.0F) event.setNewSpeed(event.getNewSpeed() * factor);
    }

    @Override
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {
        if (kind != TartariteKind.SWORD) return;
        float bonus = WearTier.swordBonus(tierOf(stack));
        if (bonus > 0.0F) event.setAmount(event.getAmount() + bonus);
    }

    @Override
    public void syncWearer(EntityPlayer player) {
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armor == null) return;
        double perTier = ArcanaQuestTweaksConfig.ParagonModuleConfig.traits.zaniteArmorPerTier;
        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack worn = player.getItemStackFromSlot(ARMOR[i]);
            double bonus = 0.0D;
            if (!worn.isEmpty() && ParagonRegistry.traitOf(worn) == this) {
                TartariteKind kind = ParagonRegistry.kindOf(worn);
                if (kind != null && kind.isArmor()) bonus = tierOf(worn) * perTier;
            }
            AttributeModifier current = armor.getModifier(MODIFIERS[i]);
            if (current != null && current.getAmount() == bonus) continue;
            if (current != null) armor.removeModifier(MODIFIERS[i]);
            if (bonus > 0.0D) armor.applyModifier(new AttributeModifier(MODIFIERS[i], "aqtweaks_zanite_" + i, bonus, 0));
        }
    }

    @Override
    public void addTooltip(ItemStack stack, TartariteKind kind, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("aqtweaks.paragon.zanite.tier", tierOf(stack), WearTier.TIERS - 1));
    }
}
