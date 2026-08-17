package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public final class EmberMotorHelper {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-EmberMotor");
    private static final String EMBERS_MODID = "embers";

    private static boolean initialized;
    private static boolean embersInstalled;
    private static boolean loaded;
    private static Method getEmberTotalMethod;
    private static Method removeEmberMethod;

    private EmberMotorHelper() {}

    private static void ensureInit() {
        if (initialized) return;
        initialized = true;
        embersInstalled = Loader.isModLoaded(EMBERS_MODID);
        if (!embersInstalled) {
            loaded = false;
            return;
        }
        try {
            Class<?> util = Class.forName("teamroots.embers.util.EmberInventoryUtil");
            getEmberTotalMethod = util.getMethod("getEmberTotal", EntityPlayer.class);
            removeEmberMethod = util.getMethod("removeEmber", EntityPlayer.class, double.class);
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
            getEmberTotalMethod = null;
            removeEmberMethod = null;
            LOGGER.warn("Embers is loaded but EmberInventoryUtil could not be bound; motor pull will stay disabled while Motor Requires Ember is true.", t);
        }
    }

    public static boolean isLoaded() {
        ensureInit();
        return loaded;
    }

    public static boolean requiresEmber() {
        ensureInit();
        return ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber && embersInstalled && loaded;
    }

    public static double getEmberTotal(EntityPlayer player) {
        ensureInit();
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
        ensureInit();
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber) return true;
        if (!embersInstalled) return true;
        if (!loaded) return false;
        if (cost <= 0.0D) return true;
        return getEmberTotal(player) >= cost;
    }

    public static boolean consumeEmber(EntityPlayer player, double amount) {
        ensureInit();
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber) return true;
        if (!embersInstalled) return true;
        if (!loaded || player == null || removeEmberMethod == null) return false;
        if (amount <= 0.0D) return true;
        if (getEmberTotal(player) < amount) return false;
        try {
            removeEmberMethod.invoke(null, player, amount);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
