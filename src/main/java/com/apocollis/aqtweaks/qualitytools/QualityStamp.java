package com.apocollis.aqtweaks.qualitytools;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.tmtravlr.qualitytools.QualityToolsHelper;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * First-gen {@code generateQualityTag(stack, false)} plus pre-damage / QualityBase.
 */
public final class QualityStamp {
    private QualityStamp() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable;
    }

    public static void stampIfUntagged(ItemStack stack) {
        if (!enabled() || stack == null || stack.isEmpty()) {
            return;
        }
        if (QualityToolsHelper.hasQualityTag(stack)) {
            return;
        }
        if (!QualityNbt.isQualityItem(stack)) {
            return;
        }
        if (!QualityToolsHelper.generateQualityTag(stack, false)) {
            return;
        }
        String color = QualityNbt.liveColor(stack);
        if (QualityNbt.COLOR_DARK_GRAY.equals(color)) {
            preDamage(stack, 0.25);
        } else if (QualityNbt.COLOR_GRAY.equals(color)) {
            preDamage(stack, 0.50);
        }
        QualityNbt.copyLiveToQualityBaseIfKept(stack);
    }

    public static void stampInventory(IInventory inventory) {
        if (!enabled() || inventory == null) {
            return;
        }
        int size = inventory.getSizeInventory();
        for (int i = 0; i < size; i++) {
            stampIfUntagged(inventory.getStackInSlot(i));
        }
    }

    private static void preDamage(ItemStack stack, double remainingFraction) {
        if (!stack.isItemStackDamageable()) {
            return;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return;
        }
        int remaining = Math.max(1, (int) Math.floor(remainingFraction * max));
        int targetDamage = max - remaining;
        if (stack.getItemDamage() < targetDamage) {
            stack.setItemDamage(targetDamage);
        }
    }
}
