package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import java.lang.reflect.Method;

public class ThaumcraftHelper {

    private static boolean initialized = false;
    private static Class<?> enumWarpTypeClass = null;
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
            Class<?> capsClass = Class.forName("thaumcraft.api.capabilities.ThaumcraftCapabilities");
            getWarpMethod = capsClass.getMethod("getWarp", EntityPlayer.class);

            enumWarpTypeClass = Class.forName("thaumcraft.api.capabilities.IPlayerWarp$EnumWarpType");
            for (Object enumConstant : enumWarpTypeClass.getEnumConstants()) {
                String name = ((Enum<?>) enumConstant).name();
                if ("NORMAL".equals(name)) {
                    warpTypeNormal = enumConstant;
                } else if ("TEMPORARY".equals(name)) {
                    warpTypeTemp = enumConstant;
                } else if ("PERMANENT".equals(name)) {
                    warpTypePerm = enumConstant;
                }
            }

            Class<?> warpCapClass = Class.forName("thaumcraft.api.capabilities.IPlayerWarp");
            getWarpValueMethod = warpCapClass.getMethod("get", enumWarpTypeClass);
            addWarpMethod = warpCapClass.getMethod("add", enumWarpTypeClass, int.class);
            reduceWarpMethod = warpCapClass.getMethod("reduce", enumWarpTypeClass, int.class);
            syncMethod = warpCapClass.getMethod("sync", EntityPlayerMP.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getWarp(EntityPlayer player, int typeIndex) {
        init();
        if (getWarpMethod == null || getWarpValueMethod == null) return 0;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                Object type = (typeIndex == 0) ? warpTypeNormal : ((typeIndex == 1) ? warpTypeTemp : warpTypePerm);
                return (Integer) getWarpValueMethod.invoke(warpCap, type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int addWarp(EntityPlayer player, int typeIndex, int amount) {
        init();
        if (getWarpMethod == null || addWarpMethod == null) return 0;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                Object type = (typeIndex == 0) ? warpTypeNormal : ((typeIndex == 1) ? warpTypeTemp : warpTypePerm);
                return (Integer) addWarpMethod.invoke(warpCap, type, amount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int reduceWarp(EntityPlayer player, int typeIndex, int amount) {
        init();
        if (getWarpMethod == null || reduceWarpMethod == null) return 0;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                Object type = (typeIndex == 0) ? warpTypeNormal : ((typeIndex == 1) ? warpTypeTemp : warpTypePerm);
                return (Integer) reduceWarpMethod.invoke(warpCap, type, amount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void syncWarp(EntityPlayer player) {
        init();
        if (getWarpMethod == null || syncMethod == null || !(player instanceof EntityPlayerMP)) return;
        try {
            Object warpCap = getWarpMethod.invoke(null, player);
            if (warpCap != null) {
                syncMethod.invoke(warpCap, (EntityPlayerMP) player);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
