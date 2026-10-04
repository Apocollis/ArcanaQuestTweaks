package com.apocollis.aqtweaks.thaumcraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.IPlayerWarp.EnumWarpType;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * Compile-hard Thaumcraft warp access (formerly raw reflection so Comfort could "call it without
 * importing TC types"). Every method is a no-op when Thaumcraft is absent; callers on always-on
 * classes (Comfort) rely on that guard, so the TC types are not touched without the mod.
 *
 * <p>A failing call is logged once and returns 0; it no longer disables the integration for the
 * rest of the session.
 */
public class ThaumcraftHelper {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Thaumcraft");

    private static final boolean LOADED = Loader.isModLoaded("thaumcraft");
    private static boolean warned = false;

    /** True when Thaumcraft is present. A broken capability is reported through the log, not here. */
    public static boolean available() {
        return LOADED;
    }

    private static EnumWarpType type(int typeIndex) {
        return typeIndex == 0 ? EnumWarpType.NORMAL : (typeIndex == 1 ? EnumWarpType.TEMPORARY : EnumWarpType.PERMANENT);
    }

    private static IPlayerWarp warp(EntityPlayer player) {
        if (!LOADED || player == null) return null;
        return ThaumcraftCapabilities.getWarp(player);
    }

    private static void fail(String op, Throwable t) {
        if (!warned) {
            warned = true;
            LOGGER.error("[AQ-TC] Thaumcraft warp {} failed; this error is logged once", op, t);
        }
    }

    public static int getWarp(EntityPlayer player, int typeIndex) {
        try {
            IPlayerWarp cap = warp(player);
            return cap != null ? cap.get(type(typeIndex)) : 0;
        } catch (RuntimeException e) {
            fail("get", e);
            return 0;
        }
    }

    public static int addWarp(EntityPlayer player, int typeIndex, int amount) {
        try {
            IPlayerWarp cap = warp(player);
            return cap != null ? cap.add(type(typeIndex), amount) : 0;
        } catch (RuntimeException e) {
            fail("add", e);
            return 0;
        }
    }

    public static int reduceWarp(EntityPlayer player, int typeIndex, int amount) {
        try {
            IPlayerWarp cap = warp(player);
            return cap != null ? cap.reduce(type(typeIndex), amount) : 0;
        } catch (RuntimeException e) {
            fail("reduce", e);
            return 0;
        }
    }

    public static void syncWarp(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        try {
            IPlayerWarp cap = warp(player);
            if (cap != null) {
                cap.sync((EntityPlayerMP) player);
            }
        } catch (RuntimeException e) {
            fail("sync", e);
        }
    }
}
