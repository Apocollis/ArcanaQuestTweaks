package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

public final class EmberMotorHelper {
    private static boolean loaded;
    private static Method getEmberTotalMethod;
    private static Method removeEmberMethod;

    static {
        try {
            if (Loader.isModLoaded("embers")) {
                Class<?> util = Class.forName("teamroots.embers.util.EmberInventoryUtil");
                getEmberTotalMethod = util.getMethod("getEmberTotal", EntityPlayer.class);
                removeEmberMethod = util.getMethod("removeEmber", EntityPlayer.class, double.class);
                loaded = true;
            }
        } catch (Throwable ignored) {
            loaded = false;
        }
    }

    private EmberMotorHelper() {}

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean requiresEmber() {
        return ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber && loaded;
    }

    public static double getEmberTotal(EntityPlayer player) {
        if (!loaded || player == null || getEmberTotalMethod == null) return 0.0D;
        try {
            Object result = getEmberTotalMethod.invoke(null, player);
            if (result instanceof Number) {
                return ((Number) result).doubleValue();
            }
        } catch (Exception ignored) {}
        return 0.0D;
    }

    public static boolean hasEmber(EntityPlayer player, double cost) {
        if (!requiresEmber()) return true;
        if (cost <= 0.0D) return true;
        return getEmberTotal(player) >= cost;
    }

    public static boolean consumeEmber(EntityPlayer player, double amount) {
        if (!requiresEmber() || player == null || amount <= 0.0D) return true;
        if (getEmberTotal(player) < amount) return false;
        try {
            removeEmberMethod.invoke(null, player, amount);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
