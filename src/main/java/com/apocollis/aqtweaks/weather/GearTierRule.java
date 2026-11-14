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
            if (allWhitelisted && !matchesStack(stack, whitelist)) {
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

    public static boolean matchesStack(ItemStack stack, String[] list) {
        if (stack == null || stack.isEmpty() || list == null || list.length == 0) return false;
        ResourceLocation reg = stack.getItem().getRegistryName();
        return reg != null && Wildcard.matchesAnyItem(reg.toString(), stack.getMetadata(), list);
    }

    /** True if every armor slot holds a piece on the list (used for insulated, channeling, and arcane sets). */
    public static boolean allSlotsOnList(EntityPlayer player, String[] list) {
        for (EntityEquipmentSlot slot : SLOTS) {
            if (!matchesStack(player.getItemStackFromSlot(slot), list)) return false;
        }
        return true;
    }

    /** Number of the four armor slots currently holding an item matching the list. */
    public static int countOnList(EntityPlayer player, String[] list) {
        int count = 0;
        for (EntityEquipmentSlot slot : SLOTS) {
            if (matchesStack(player.getItemStackFromSlot(slot), list)) count++;
        }
        return count;
    }

    /** True if the specified equipment slot holds an item matching the list. */
    public static boolean slotMatches(EntityPlayer player, EntityEquipmentSlot slot, String[] list) {
        return matchesStack(player.getItemStackFromSlot(slot), list);
    }

    /** True if any armor slot or either hand holds an item on the list. */
    public static boolean anyOnList(EntityPlayer player, String[] list) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (matchesStack(player.getItemStackFromSlot(slot), list)) return true;
        }
        return false;
    }

    /**
     * True if any armor slot or either hand holds an item matching {@code list} that does not match
     * any of {@code excluded} (so Insulated or Channeling pieces never also count as Conductive).
     */
    public static boolean anyOnListExcluding(EntityPlayer player, String[] list, String[]... excluded) {
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (!matchesStack(stack, list)) continue;
            boolean isExcluded = false;
            if (excluded != null) {
                for (String[] ex : excluded) {
                    if (matchesStack(stack, ex)) {
                        isExcluded = true;
                        break;
                    }
                }
            }
            if (!isExcluded) return true;
        }
        return false;
    }
}
