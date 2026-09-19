package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;

public final class ThaumcraftPerkHooks {

    private static final ThreadLocal<Integer> WARP_BOUND = new ThreadLocal<>();

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
}
