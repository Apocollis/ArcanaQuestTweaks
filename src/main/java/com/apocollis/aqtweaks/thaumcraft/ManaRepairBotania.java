package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import vazkii.botania.api.mana.ManaItemHandler;

/** Spends the player's Botania mana on repairing and protecting Mana-Weave items, at Botania's own prices. */
public final class ManaRepairBotania {
    private ManaRepairBotania() {}

    private static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof ItemArmor;
    }

    /** Pays for {@code amount} points of damage with mana. All or nothing, as Botania does. Server side only. */
    public static boolean payDamage(EntityPlayer player, ItemStack stack, int amount) {
        if (amount <= 0 || player.world.isRemote || player.capabilities.isCreativeMode) return false;
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableManaRepair
                || !ArcanaQuestTweaksConfig.ThaumcraftConfig.enableManaRepairMitigation) return false;
        boolean armor = isArmor(stack);
        int cost = ManaRepairLogic.damageCost(amount, armor, ArcanaQuestTweaksConfig.ThaumcraftConfig.manaRepairCostMultiplier);
        if (cost <= 0) return true;
        return armor
                ? ManaItemHandler.requestManaExact(stack, player, cost, true)
                : ManaItemHandler.requestManaExactForTool(stack, player, cost, true);
    }

    /** Repairs one durability point of every tagged, damaged item the player carries; armor only while worn. */
    public static void repairInventory(EntityPlayer player) {
        double multiplier = ArcanaQuestTweaksConfig.ThaumcraftConfig.manaRepairCostMultiplier;
        repairList(player, player.inventory.mainInventory, false, multiplier);
        repairList(player, player.inventory.offHandInventory, false, multiplier);
        repairList(player, player.inventory.armorInventory, true, multiplier);
    }

    private static void repairList(EntityPlayer player, NonNullList<ItemStack> stacks, boolean worn, double multiplier) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty() || !stack.isItemDamaged() || !ManaRepairTag.has(stack)) continue;
            boolean armor = isArmor(stack);
            if (armor && !worn) continue;
            int cost = ManaRepairLogic.repairCost(armor, multiplier);
            boolean paid = cost <= 0 || (armor
                    ? ManaItemHandler.requestManaExact(stack, player, cost, true)
                    : ManaItemHandler.requestManaExactForTool(stack, player, cost, true));
            if (paid) stack.setItemDamage(stack.getItemDamage() - 1);
        }
    }
}
