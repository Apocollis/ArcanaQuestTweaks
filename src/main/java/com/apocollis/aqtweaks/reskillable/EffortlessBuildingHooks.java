package com.apocollis.aqtweaks.reskillable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * Called from the optional EB mixin. Does not import Reskillable types so the mixin
 * class can load when Effortless Building is present and Reskillable is not.
 */
public final class EffortlessBuildingHooks {

    private static Method placeReach;
    private static Method maxBlocks;
    private static boolean resolved;

    private EffortlessBuildingHooks() {}

    public static int placementReach(EntityPlayer player, int base) {
        return invoke(placeReachMethod(), player, base);
    }

    public static int maxBlocksPlaced(EntityPlayer player, int base) {
        return invoke(maxBlocksMethod(), player, base);
    }

    private static int invoke(Method method, EntityPlayer player, int base) {
        if (method == null) return base;
        try {
            Object out = method.invoke(null, player, base);
            return out instanceof Integer ? (Integer) out : base;
        } catch (Throwable ignored) {
            return base;
        }
    }

    private static Method placeReachMethod() {
        resolve();
        return placeReach;
    }

    private static Method maxBlocksMethod() {
        resolve();
        return maxBlocks;
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!Loader.isModLoaded("reskillable")) return;
        try {
            Class<?> bonuses = Class.forName("com.apocollis.aqtweaks.reskillable.ReskillableBonuses");
            placeReach = bonuses.getMethod("addBuildingPlaceReach", EntityPlayer.class, int.class);
            maxBlocks = bonuses.getMethod("addBuildingMaxBlocks", EntityPlayer.class, int.class);
        } catch (Throwable ignored) {
            placeReach = null;
            maxBlocks = null;
        }
    }
}
