package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public class ThaumcraftHelper {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Thaumcraft");

    /** Set once if an invoke against the TC warp capability blows up, so we stop pretending it works. */
    private static boolean apiBroken = false;
    private static boolean warnedBroken = false;

    private static boolean initialized = false;
    private static Class enumWarpTypeClass = null;
    private static Object warpTypeNormal = null;
    private static Object warpTypeTemp = null;
    private static Object warpTypePerm = null;
    private static Method getWarpMethod = null;
    private static Method getWarpValueMethod = null;
    private static Method addWarpMethod = null;
    private static Method reduceWarpMethod = null;
    private static Method syncMethod = null;

    public static void init() {
        if (initialized) return;
        initialized = true;
        if (!Loader.isModLoaded("thaumcraft")) return;

        try {
            Class capsClass = Class.forName("thaumcraft.api.capabilities.ThaumcraftCapabilities");
            getWarpMethod = capsClass.getMethod("getWarp", EntityPlayer.class);

            enumWarpTypeClass = Class.forName("thaumcraft.api.capabilities.IPlayerWarp$EnumWarpType");
            Object[] enumConstants = enumWarpTypeClass.getEnumConstants();
            if (enumConstants != null) {
                for (int i = 0; i < enumConstants.length; i++) {
                    Object enumConstant = enumConstants[i];
                    String name = ((Enum) enumConstant).name();
                    if ("NORMAL".equals(name)) {
                        warpTypeNormal = enumConstant;
                    } else if ("TEMPORARY".equals(name)) {
                        warpTypeTemp = enumConstant;
                    } else if ("PERMANENT".equals(name)) {
                        warpTypePerm = enumConstant;
                    }
                }
            }

            Class warpCapClass = Class.forName("thaumcraft.api.capabilities.IPlayerWarp");
            getWarpValueMethod = warpCapClass.getMethod("get", enumWarpTypeClass);
            addWarpMethod = warpCapClass.getMethod("add", enumWarpTypeClass, int.class);
            reduceWarpMethod = warpCapClass.getMethod("reduce", enumWarpTypeClass, int.class);
            syncMethod = warpCapClass.getMethod("sync", EntityPlayerMP.class);
        } catch (Exception e) {
            apiBroken = true;
            LOGGER.error("[AQ-TC] Could not bind the Thaumcraft warp capability; all Tweaks warp "
                    + "sources and sinks are disabled for this session", e);
        }
    }

    /**
     * False when the warp capability could not be bound or has since thrown. Callers must check this
     * before treating a {@code 0} return as "the player has no warp" — otherwise a broken API reads
     * as a successful cleanse.
     */
    public static boolean available() {
        init();
        return !apiBroken && getWarpMethod != null && getWarpValueMethod != null;
    }

    private static void markBroken(String op, Throwable t) {
        apiBroken = true;
        if (!warnedBroken) {
            warnedBroken = true;
            LOGGER.error("[AQ-TC] Thaumcraft warp {} failed; disabling Tweaks warp integration "
                    + "for this session", op, t);
        }
    }

    public static int getWarp(EntityPlayer player, int typeIndex) {
        init();
        if (apiBroken || getWarpMethod == null || getWarpValueMethod == null) return 0;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                Object type = (typeIndex == 0) ? warpTypeNormal : ((typeIndex == 1) ? warpTypeTemp : warpTypePerm);
                return (Integer) getWarpValueMethod.invoke(warpCap, type);
            }
        } catch (Exception e) {
            markBroken("get", e);
        }
        return 0;
    }

    public static int addWarp(EntityPlayer player, int typeIndex, int amount) {
        init();
        if (apiBroken || getWarpMethod == null || addWarpMethod == null) return 0;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                Object type = (typeIndex == 0) ? warpTypeNormal : ((typeIndex == 1) ? warpTypeTemp : warpTypePerm);
                return (Integer) addWarpMethod.invoke(warpCap, type, amount);
            }
        } catch (Exception e) {
            markBroken("add", e);
        }
        return 0;
    }

    public static int reduceWarp(EntityPlayer player, int typeIndex, int amount) {
        init();
        if (apiBroken || getWarpMethod == null || reduceWarpMethod == null) return 0;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                Object type = (typeIndex == 0) ? warpTypeNormal : ((typeIndex == 1) ? warpTypeTemp : warpTypePerm);
                return (Integer) reduceWarpMethod.invoke(warpCap, type, amount);
            }
        } catch (Exception e) {
            markBroken("reduce", e);
        }
        return 0;
    }

    public static void syncWarp(EntityPlayer player) {
        init();
        if (apiBroken || getWarpMethod == null || syncMethod == null || !(player instanceof EntityPlayerMP)) return;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                syncMethod.invoke(warpCap, (EntityPlayerMP) player);
            }
        } catch (Exception e) {
            markBroken("sync", e);
        }
    }
}
