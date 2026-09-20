package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;
import thaumcraft.common.world.aura.AuraHandler;

public final class ThaumcraftPerkHooks {

    private static final ThreadLocal<Integer> WARP_BOUND = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> FULL_FONT_APPLIED = new ThreadLocal<>();

    private ThaumcraftPerkHooks() {}

    public static float visDiscount(EntityPlayer player, float base) {
        if (player == null || player instanceof FakePlayer) return base;
        if (!Reflect.hasUnlockable(player, "aqtweaks:vis_thrift")) return base;
        double add = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.visThriftAdd;
        return (float) Math.min(0.9, base + add);
    }

    public static int captureWarpBound(int bound) {
        WARP_BOUND.set(bound);
        return bound;
    }

    public static int quietMindSeverity(EntityPlayer player, int event) {
        Integer bound = WARP_BOUND.get();
        WARP_BOUND.remove();
        if (player == null || player instanceof FakePlayer) return event;
        if (!Reflect.hasUnlockable(player, "aqtweaks:quiet_mind")) return event;
        int usedBound = bound != null ? bound : 0;
        if (usedBound <= 0) return event;
        double frac = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.quietMindBoundFraction;
        int reduced = event - (int) Math.round(frac * usedBound);
        if (reduced <= 0 && !player.world.isRemote) {
            String msg = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.quietMindCancelChat;
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + msg));
            }
        }
        return reduced;
    }

    public static float fullFontCost(EntityPlayer player, float amount, boolean crafting) {
        FULL_FONT_APPLIED.remove();
        if (crafting || player == null || player instanceof FakePlayer) return amount;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.fullFont.enable) return amount;
        if (!Reflect.hasUnlockable(player, "aqtweaks:full_font")) return amount;
        if (!auraFull(player)) return amount;
        FULL_FONT_APPLIED.set(Boolean.TRUE);
        return (float) (amount * ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.fullFontVisCost);
    }

    public static void fullFontStamp(EntityPlayer player, boolean crafting) {
        boolean applied = Boolean.TRUE.equals(FULL_FONT_APPLIED.get());
        FULL_FONT_APPLIED.remove();
        if (!applied || crafting || player == null || player instanceof FakePlayer) return;
        try {
            Class<?> bonuses = Class.forName("com.apocollis.aqtweaks.reskillable.ReskillableBonuses");
            bonuses.getMethod("stampFullFont", EntityPlayer.class).invoke(null, player);
        } catch (Throwable ignored) {
        }
    }

    private static boolean auraFull(EntityPlayer player) {
        if (player.world == null) return false;
        BlockPos pos = player.getPosition();
        float vis = AuraHandler.getVis(player.world, pos);
        int base = AuraHandler.getAuraBase(player.world, pos);
        if (base <= 0) return false;
        double ratio = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.fullFontAuraRatio;
        return vis >= base * ratio;
    }
}
