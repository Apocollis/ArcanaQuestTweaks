package com.apocollis.aqtweaks.reskillable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Called from the optional EB mixin. Does not import Reskillable types so the mixin
 * class can load when Effortless Building is present and Reskillable is not.
 */
public final class EffortlessBuildingHooks {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Reskillable");

    private static Method placeReach;
    private static Method maxBlocks;
    private static boolean resolved;
    private static boolean warnedInvoke;

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
        } catch (Throwable t) {
            if (!warnedInvoke) {
                warnedInvoke = true;
                LOGGER.warn("[AQ-EB] Building bonus bridge threw; falling back to Effortless Building's "
                        + "own values for this session", t);
            }
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
        } catch (Throwable t) {
            placeReach = null;
            maxBlocks = null;
            LOGGER.warn("[AQ-EB] Reskillable is loaded but the Building bonus bridge could not be "
                    + "resolved; place reach and max blocks will use Effortless Building's values", t);
        }
    }

    public static String clampBuildMode(EntityPlayer player, String modeName) {
        if (modeName == null || "NORMAL".equals(modeName) || "NORMAL_PLUS".equals(modeName)) {
            return modeName == null ? "NORMAL" : modeName;
        }
        return switch (modeName) {
            case "LINE", "WALL", "FLOOR", "DIAGONAL_LINE", "DIAGONAL_WALL", "SLOPE_FLOOR" ->
                    com.apocollis.aqtweaks.util.Reflect.hasUnlockable(player, "aqtweaks:drafter")
                            ? modeName : "NORMAL";
            case "CIRCLE", "CYLINDER", "SPHERE", "CUBE" ->
                    com.apocollis.aqtweaks.util.Reflect.hasUnlockable(player, "aqtweaks:sculptor")
                            ? modeName : "NORMAL";
            default -> modeName;
        };
    }

    public static boolean allowQuickReplace(EntityPlayer player) {
        return com.apocollis.aqtweaks.util.Reflect.hasUnlockable(player, "aqtweaks:transpose");
    }
}
