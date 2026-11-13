package com.apocollis.aqtweaks.weather;

import com.apocollis.aqtweaks.aurorian.MoonfrostTier;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

/**
 * The three-tier whole-set rule shared by the dimension hazards (Atum's rule): EXPOSED if any armor slot is
 * empty, PROPER if the named effect is active with four armor pieces worn or all four pieces are on the
 * whitelist, otherwise ARMORED. Effects are looked up by registry name, so no parent mod is imported.
 */
public final class GearTierRule {
    private static final EntityEquipmentSlot[] SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
    };

    private GearTierRule() {}

    public static MoonfrostTier tierOf(EntityPlayer player, String effectName, String[] whitelist) {
        boolean allArmor = true;
        boolean allWhitelisted = true;
        for (EntityEquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemArmor)) {
                allArmor = false;
                allWhitelisted = false;
                break;
            }
            ResourceLocation reg = stack.getItem().getRegistryName();
            if (allWhitelisted && (reg == null || !Wildcard.matchesAny(reg.toString(), whitelist))) {
                allWhitelisted = false;
            }
        }
        return MoonfrostTier.classify(allArmor, allArmor && hasEffect(player, effectName), allWhitelisted);
    }

    public static boolean hasEffect(EntityPlayer player, String effectName) {
        if (effectName == null || effectName.trim().isEmpty()) return false;
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(effectName.trim()));
        return potion != null && player.isPotionActive(potion);
    }

    /** True if every armor slot holds a piece on the list (used for the Aether insulated set). */
    public static boolean allSlotsOnList(EntityPlayer player, String[] list) {
        for (EntityEquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty()) return false;
            ResourceLocation reg = stack.getItem().getRegistryName();
            if (reg == null || !Wildcard.matchesAny(reg.toString(), list)) return false;
        }
        return true;
    }

    /** True if any armor slot or either hand holds an item on the list. */
    public static boolean anyOnList(EntityPlayer player, String[] list) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty()) continue;
            ResourceLocation reg = stack.getItem().getRegistryName();
            if (reg != null && Wildcard.matchesAny(reg.toString(), list)) return true;
        }
        return false;
    }
}
