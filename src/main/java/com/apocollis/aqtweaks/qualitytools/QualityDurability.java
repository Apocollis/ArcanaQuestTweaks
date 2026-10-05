package com.apocollis.aqtweaks.qualitytools;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

public final class QualityDurability {
    private static final Logger LOGGER = LogManager.getLogger("aqtweaks");
    private static final ResourceLocation SALVAGE = new ResourceLocation("charm", "salvage");
    private static final ThreadLocal<Boolean> SET_DAMAGE_REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> SKIP_REPAIR = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> IN_DAMAGE_ITEM = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> KEEP_ONE_USE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static boolean loggedMissingGray;
    private static boolean loggedMissingDarkGray;

    private QualityDurability() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable;
    }

    /**
     * @return true if the vanilla destroy result should be cancelled
     */
    public static boolean afterAttemptDamage(ItemStack stack, boolean wouldDestroy, EntityPlayer player, Random rand) {
        if (!enabled()) {
            trace(stack, player, wouldDestroy, "skip:disabled", "");
            return false;
        }
        if (stack == null || stack.isEmpty()) {
            trace(stack, player, wouldDestroy, "skip:empty", "");
            return false;
        }
        if (player != null) {
            if (player.world.isRemote) {
                trace(stack, player, wouldDestroy, "skip:remote", "");
                return false;
            }
            if (player.capabilities.isCreativeMode) {
                trace(stack, player, wouldDestroy, "skip:creative", "");
                return false;
            }
        } else if (FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER) {
            trace(stack, null, wouldDestroy, "skip:remote", "");
            return false;
        }
        if (!QualityNbt.isQualityItem(stack)) {
            trace(stack, player, wouldDestroy, "skip:not_quality",
                    traceOn() ? "types=" + (com.tmtravlr.qualitytools.config.ConfigLoader.qualityTypes != null) : "");
            return false;
        }

        if (QualityNbt.isDarkGray(stack)) {
            if (wouldDestroy) {
                trace(stack, player, wouldDestroy, "break_destroy", "");
            } else {
                trace(stack, player, wouldDestroy, "skip:broken_live", "");
            }
            return false;
        }

        if (wouldDestroy) {
            if (hasSalvage(stack)) {
                trace(stack, player, wouldDestroy, "skip:salvage", "");
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
                trace(stack, player, wouldDestroy, "skip:no_dark_gray", "");
                return false;
            }
            applyOneUseLeft(stack);
            if (stack.getCount() <= 0) {
                stack.setCount(1);
            }
            if (Boolean.TRUE.equals(IN_DAMAGE_ITEM.get())) {
                KEEP_ONE_USE.set(Boolean.TRUE);
            }
            if (player != null) {
                ItemStack drop = stack.copy();
                stack.setCount(0);
                player.dropItem(drop, false);
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.5F,
                        1.5F + (player.world.rand.nextFloat() * 0.3F - 0.15F));
                trace(drop, player, wouldDestroy, "break_drop", traceOn() ? "live=" + QualityNbt.liveColor(drop) : "");
            } else {
                trace(stack, null, wouldDestroy, "break_clamp", traceOn() ? "live=" + QualityNbt.liveColor(stack) : "");
            }
            return true;
        }

        maybeWear(stack, player, rand);
        return false;
    }

    public static void onDamageItemHead() {
        IN_DAMAGE_ITEM.set(Boolean.TRUE);
    }

    public static void onDamageItemReturn(ItemStack stack) {
        try {
            if (!Boolean.TRUE.equals(KEEP_ONE_USE.get())) {
                return;
            }
            if (stack == null || stack.isEmpty() || !QualityNbt.isDarkGray(stack)) {
                return;
            }
            applyOneUseLeft(stack);
        } finally {
            KEEP_ONE_USE.set(Boolean.FALSE);
            IN_DAMAGE_ITEM.set(Boolean.FALSE);
        }
    }

    public static void afterSetDamage(ItemStack stack, int damage) {
        if (Boolean.TRUE.equals(SET_DAMAGE_REENTRY.get()) || Boolean.TRUE.equals(SKIP_REPAIR.get())) {
            return;
        }
        if (!enabled() || stack == null || stack.isEmpty() || !stack.isItemStackDamageable()) {
            return;
        }
        SET_DAMAGE_REENTRY.set(Boolean.TRUE);
        try {
            if (!QualityNbt.isQualityItem(stack)) {
                return;
            }
            int max = stack.getMaxDamage();
            if (max <= 0) {
                return;
            }
            double remaining = (max - damage) / (double) max;
            var cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
            if (remaining > cfg.lowDurability) {
                QualityNbt.setWearFlag(stack, false);
            }
            String live = QualityNbt.liveColor(stack);
            if (remaining > cfg.clearWear) {
                if (QualityNbt.COLOR_GRAY.equals(live) || QualityNbt.COLOR_DARK_GRAY.equals(live)) {
                    QualityNbt.restoreFromBaseOrStrip(stack);
                }
            } else if (remaining >= cfg.brokenToGray && remaining < cfg.clearWear
                    && QualityNbt.COLOR_DARK_GRAY.equals(live)) {
                if (!QualityNbt.applyUniqueColor(stack, QualityNbt.COLOR_GRAY)) {
                    if (!loggedMissingGray) {
                        loggedMissingGray = true;
                        LOGGER.warn("Quality Tools Module: no gray (wear) entry for {}", stack.getItem().getRegistryName());
                    }
                }
            }
        } finally {
            SET_DAMAGE_REENTRY.set(Boolean.FALSE);
        }
    }

    private static void maybeWear(ItemStack stack, EntityPlayer player, Random rand) {
        if (QualityNbt.isDarkGray(stack)) {
            trace(stack, player, false, "skip:broken_live", "");
            return;
        }
        int max = stack.getMaxDamage();
        if (max <= 0) {
            trace(stack, player, false, "skip:no_max", "");
            return;
        }
        int damage = stack.getItemDamage();
        double remaining = (max - damage) / (double) max;
        var cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        if (remaining > cfg.lowDurability) {
            QualityNbt.setWearFlag(stack, false);
            trace(stack, player, false, "skip:not_low_band", "");
            return;
        }
        if (QualityNbt.hasWearFlag(stack) || QualityNbt.isGray(stack)) {
            trace(stack, player, false, "skip:already_worn", "");
            return;
        }
        if (player != null) {
            long now = player.world.getTotalWorldTime();
            if (now - QualityNbt.lastWearCheck(stack) < cfg.wearCheckIntervalTicks) {
                trace(stack, player, false, "skip:interval", "");
                return;
            }
            QualityNbt.setLastWearCheck(stack, now);
        }
        double p = wearProbability(damage, max, cfg);
        Random rollSrc = rand != null ? rand : (player != null ? player.world.rand : new Random());
        double roll = rollSrc.nextDouble();
        if (roll >= p) {
            trace(stack, player, false, "skip:fail_roll", traceOn() ? String.format("p=%.3f roll=%.3f", p, roll) : "");
            return;
        }
        if (!QualityNbt.applyUniqueColor(stack, QualityNbt.COLOR_GRAY)) {
            if (!loggedMissingGray) {
                loggedMissingGray = true;
                LOGGER.warn("Quality Tools Module: no gray (wear) entry for {}", stack.getItem().getRegistryName());
            }
            trace(stack, player, false, "skip:no_gray", traceOn() ? String.format("p=%.3f roll=%.3f", p, roll) : "");
            return;
        }
        QualityNbt.setWearFlag(stack, true);
        trace(stack, player, false, "wear", traceOn() ? String.format("p=%.3f roll=%.3f live=%s", p, roll, QualityNbt.liveColor(stack)) : "");
    }

    static double wearProbability(int damage, int max, ArcanaQuestTweaksConfig.QualityToolsGeneral cfg) {
        if (cfg.wearChance <= 0.0 || max <= 0) {
            return 0.0;
        }
        double used = damage / (double) max;
        double halfMax = Math.max(1.0, max / 2.0);
        double p = used * (cfg.wearDurabilityRef / halfMax) * cfg.wearChance;
        p = Math.max(p, cfg.wearChanceFloor);
        p = Math.min(p, cfg.wearChanceCeiling);
        if (p < 0.0) {
            return 0.0;
        }
        if (p > 1.0) {
            return 1.0;
        }
        return p;
    }

    private static void applyOneUseLeft(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 1) {
            return;
        }
        SKIP_REPAIR.set(Boolean.TRUE);
        try {
            stack.setItemDamage(max - 1);
        } finally {
            SKIP_REPAIR.set(Boolean.FALSE);
        }
    }

    private static boolean hasSalvage(ItemStack stack) {
        Enchantment salvage = ForgeRegistries.ENCHANTMENTS.getValue(SALVAGE);
        return salvage != null && EnchantmentHelper.getEnchantmentLevel(salvage, stack) > 0;
    }

    private static boolean traceOn() {
        return ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.traceWear;
    }

    private static void trace(ItemStack stack, EntityPlayer player, boolean wouldDestroy, String outcome, String extra) {
        if (!traceOn()) return;
        int damage = 0;
        int max = 0;
        String id = "?";
        String live = "?";
        boolean quality = false;
        boolean wear = false;
        if (stack != null && !stack.isEmpty()) {
            id = String.valueOf(stack.getItem().getRegistryName());
            max = stack.getMaxDamage();
            damage = stack.getItemDamage();
            live = QualityNbt.liveColor(stack);
            quality = QualityNbt.isQualityItem(stack);
            wear = QualityNbt.hasWearFlag(stack);
        }
        LOGGER.info("quality-wear {} {} dmg={}/{} wouldDestroy={} player={} remote={} live={} quality={} wearFlag={} {}",
                outcome,
                id,
                damage,
                max,
                wouldDestroy,
                player != null,
                player != null && player.world != null && player.world.isRemote,
                live,
                quality,
                wear,
                extra);
    }
}
