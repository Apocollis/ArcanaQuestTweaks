package com.apocollis.aqtweaks.reskillable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Called from the optional EB mixin. Reskillable is called directly through {@link ReskillableBonuses},
 * guarded by {@link #RESKILLABLE}, so this class loads when Effortless Building is present and
 * Reskillable is not.
 */
public final class EffortlessBuildingHooks {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Reskillable");

    private static final boolean RESKILLABLE = Loader.isModLoaded("reskillable");
    private static boolean warnedInvoke;

    private EffortlessBuildingHooks() {}

    public static int placementReach(EntityPlayer player, int base) {
        if (!RESKILLABLE) return base;
        try {
            return ReskillableBonuses.addBuildingPlaceReach(player, base);
        } catch (RuntimeException | LinkageError t) {
            return failed(t, base);
        }
    }

    /**
     * Reach tier from the Reach I/II/III perks. With {@code Disable EB Reach Upgrade Items} the stored
     * upgrade level is ignored: the perk tier is the only source. Creative is untouched.
     */
    public static int maxReach(EntityPlayer player, int base) {
        if (!RESKILLABLE || player == null || player.isCreative()) return base;
        var building = com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ReskillableModuleConfig.building;
        if (!building.disableReachUpgradeItems) return base;
        var reach = nl.requios.effortlessbuilding.BuildConfig.reach;
        if (!reach.enableReachUpgrades) return base;
        var perks = com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        if (perks.reach3.enable && PerkAccess.has(player, "aqtweaks:reach3")) return reach.maxReachLevel3;
        if (perks.reach2.enable && PerkAccess.has(player, "aqtweaks:reach2")) return reach.maxReachLevel2;
        if (perks.reach1.enable && PerkAccess.has(player, "aqtweaks:reach1")) return reach.maxReachLevel1;
        return reach.maxReachLevel0;
    }

    public static int maxBlocksPlaced(EntityPlayer player, int base) {
        if (!RESKILLABLE) return base;
        try {
            return ReskillableBonuses.addBuildingMaxBlocks(player, base);
        } catch (RuntimeException | LinkageError t) {
            return failed(t, base);
        }
    }

    private static int failed(Throwable t, int base) {
        if (!warnedInvoke) {
            warnedInvoke = true;
            LOGGER.warn("[AQ-EB] Building bonus threw; falling back to Effortless Building's "
                    + "own values for this session", t);
        }
        return base;
    }

    public static String clampBuildMode(EntityPlayer player, String modeName) {
        if (modeName == null || "NORMAL".equals(modeName) || "NORMAL_PLUS".equals(modeName)) {
            return modeName == null ? "NORMAL" : modeName;
        }
        return switch (modeName) {
            case "LINE", "WALL", "FLOOR", "DIAGONAL_LINE", "DIAGONAL_WALL", "SLOPE_FLOOR" ->
                    PerkAccess.has(player, "aqtweaks:drafter")
                            ? modeName : "NORMAL";
            case "CIRCLE", "CYLINDER", "SPHERE", "CUBE" ->
                    PerkAccess.has(player, "aqtweaks:sculptor")
                            ? modeName : "NORMAL";
            default -> modeName;
        };
    }

    public static boolean allowQuickReplace(EntityPlayer player) {
        return PerkAccess.has(player, "aqtweaks:transpose");
    }
}
