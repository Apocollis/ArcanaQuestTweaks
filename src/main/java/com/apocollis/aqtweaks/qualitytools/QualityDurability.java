package com.apocollis.aqtweaks.qualitytools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class QualityDurability {
    private static final Logger LOGGER = LogManager.getLogger("aqtweaks");
    private static final ResourceLocation SALVAGE = new ResourceLocation("charm", "salvage");
    private static boolean loggedMissingGray;
    private static boolean loggedMissingDarkGray;

    private QualityDurability() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable;
    }

    /**
     * @return true if the vanilla destroy result should be cancelled
     */
    public static boolean afterAttemptDamage(ItemStack stack, boolean wouldDestroy, EntityPlayerMP player) {
        if (!enabled() || stack == null || stack.isEmpty() || player == null || player.world.isRemote) {
            return false;
        }
        if (player.capabilities.isCreativeMode) {
            return false;
        }
        if (!QualityNbt.isQualityItem(stack)) {
            return false;
        }

        if (QualityNbt.isDarkGray(stack)) {
            if (wouldDestroy) {
                clampToOneUse(stack);
                return true;
            }
            return false;
        }

        if (wouldDestroy) {
            if (hasSalvage(stack)) {
                return false;
            }
            if (QualityNbt.getQualityBase(stack) == null && !QualityNbt.isGray(stack)) {
                QualityNbt.copyLiveToQualityBaseIfKept(stack);
            }
            if (!QualityNbt.applyUniqueColor(stack, QualityNbt.COLOR_DARK_GRAY)) {
                if (!loggedMissingDarkGray) {
                    loggedMissingDarkGray = true;
                    LOGGER.warn("Quality Tools Module: no dark_gray (Broken) entry for {}", stack.getItem().getRegistryName());
                }
                return false;
            }
            clampToOneUse(stack);
            ItemStack drop = stack.copy();
            stack.setCount(0);
            player.dropItem(drop, false);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.5F,
                    1.5F + (player.world.rand.nextFloat() * 0.3F - 0.15F));
            return true;
        }

        maybeWear(stack, player);
        return false;
    }

    public static void afterSetDamage(ItemStack stack) {
        if (!enabled() || stack == null || stack.isEmpty() || !stack.isItemStackDamageable()) {
            return;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return;
        }
        double remaining = (max - stack.getItemDamage()) / (double) max;
        var cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        if (remaining > cfg.lowDurability) {
            QualityNbt.setWearFlag(stack, false);
        }
        if (remaining >= cfg.highDurability) {
            String live = QualityNbt.liveColor(stack);
            if (QualityNbt.COLOR_GRAY.equals(live) || QualityNbt.COLOR_DARK_GRAY.equals(live)) {
                QualityNbt.restoreFromBaseOrStrip(stack);
            }
        }
    }

    private static void maybeWear(ItemStack stack, EntityPlayer player) {
        if (QualityNbt.isDarkGray(stack)) {
            return;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return;
        }
        double remaining = (max - stack.getItemDamage()) / (double) max;
        var cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        if (remaining > cfg.lowDurability) {
            QualityNbt.setWearFlag(stack, false);
            return;
        }
        if (QualityNbt.hasWearFlag(stack) || QualityNbt.isGray(stack)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        if (now - QualityNbt.lastWearCheck(stack) < cfg.wearCheckIntervalTicks) {
            return;
        }
        QualityNbt.setLastWearCheck(stack, now);
        if (player.world.rand.nextDouble() > cfg.wearChance) {
            return;
        }
        if (!QualityNbt.applyUniqueColor(stack, QualityNbt.COLOR_GRAY)) {
            if (!loggedMissingGray) {
                loggedMissingGray = true;
                    LOGGER.warn("Quality Tools Module: no gray (wear) entry for {}", stack.getItem().getRegistryName());
            }
            return;
        }
        QualityNbt.setWearFlag(stack, true);
    }

    private static void clampToOneUse(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max > 0) {
            stack.setItemDamage(Math.max(0, max - 1));
        }
        if (stack.getCount() <= 0) {
            stack.setCount(1);
        }
    }

    private static boolean hasSalvage(ItemStack stack) {
        Enchantment salvage = ForgeRegistries.ENCHANTMENTS.getValue(SALVAGE);
        return salvage != null && EnchantmentHelper.getEnchantmentLevel(salvage, stack) > 0;
    }
}
