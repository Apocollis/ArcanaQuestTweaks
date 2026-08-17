package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SideOnly(Side.CLIENT)
public final class GrappleClientInput {
    public static final int MODE_NEUTRAL = PacketSyncGrappleInput.MODE_NEUTRAL;
    public static final int MODE_CLIMB = PacketSyncGrappleInput.MODE_CLIMB;
    public static final int MODE_DESCEND = PacketSyncGrappleInput.MODE_DESCEND;
    public static final int MODE_SWING = PacketSyncGrappleInput.MODE_SWING;

    private static boolean initialized;
    private static Field keyClimbField;
    private static Field keyClimbUpField;
    private static Field keyClimbDownField;
    private static Field keyMotorField;
    private static Field customMotorField;
    private static Field customMotorWhenCrouchingField;
    private static Field customMotorWhenNotCrouchingField;
    private static Field controllerForwardField;
    private static Field controllerSneakField;
    private static Field controllerOnGroundTimerField;
    private static Method keyIsKeyDownMethod;
    private static Method keyGetKeyCodeMethod;

    private GrappleClientInput() {}

    private static void ensureInit() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> clientProxy = Class.forName("com.yyon.grapplinghook.ClientProxyClass");
            keyClimbField = clientProxy.getField("key_climb");
            keyClimbUpField = clientProxy.getField("key_climbup");
            keyClimbDownField = clientProxy.getField("key_climbdown");
            keyMotorField = clientProxy.getField("key_motoronoff");

            Class<?> customClass = Class.forName("com.yyon.grapplinghook.GrappleCustomization");
            customMotorField = customClass.getField("motor");
            customMotorWhenCrouchingField = customClass.getField("motorwhencrouching");
            customMotorWhenNotCrouchingField = customClass.getField("motorwhennotcrouching");

            Class<?> controllerClass = Class.forName("com.yyon.grapplinghook.controllers.grappleController");
            controllerForwardField = controllerClass.getField("playerforward");
            controllerSneakField = controllerClass.getField("playersneak");
            controllerOnGroundTimerField = controllerClass.getField("ongroundtimer");

            Object sampleKey = keyClimbField.get(null);
            Class<?> keyClass = sampleKey != null
                    ? sampleKey.getClass()
                    : Class.forName("net.minecraft.client.settings.KeyBinding");
            Class<?> vanillaKey = Class.forName("net.minecraft.client.settings.KeyBinding");
            try {
                keyIsKeyDownMethod = keyClass.getMethod("func_151470_d");
            } catch (NoSuchMethodException e) {
                try {
                    keyIsKeyDownMethod = keyClass.getMethod("isKeyDown");
                } catch (NoSuchMethodException e2) {
                    keyIsKeyDownMethod = vanillaKey.getMethod("isKeyDown");
                }
            }
            try {
                keyGetKeyCodeMethod = keyClass.getMethod("func_151463_i");
            } catch (NoSuchMethodException e) {
                try {
                    keyGetKeyCodeMethod = keyClass.getMethod("getKeyCode");
                } catch (NoSuchMethodException e2) {
                    keyGetKeyCodeMethod = vanillaKey.getMethod("getKeyCode");
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean isKeyDown(Field keyField) {
        if (keyField == null || keyIsKeyDownMethod == null) return false;
        try {
            Object key = keyField.get(null);
            if (key == null) return false;
            Object result = keyIsKeyDownMethod.invoke(key);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isBoundKeyDown(Field keyField) {
        if (keyField == null || keyGetKeyCodeMethod == null) return false;
        try {
            Object key = keyField.get(null);
            if (key == null) return false;
            Object code = keyGetKeyCodeMethod.invoke(key);
            int keyCode = code instanceof Integer ? (Integer) code : 0;
            if (keyCode == 0) return false;
            return isKeyDown(keyField);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isMotorPulling(EntityPlayer player) {
        ensureInit();
        Object custom = Reflect.getGrappleCustomization(player);
        if (custom == null || customMotorField == null) return false;
        try {
            if (!customMotorField.getBoolean(custom)) return false;
            boolean motorKey = isKeyDown(keyMotorField);
            boolean whenCrouching = customMotorWhenCrouchingField.getBoolean(custom);
            boolean whenNotCrouching = customMotorWhenNotCrouchingField.getBoolean(custom);
            boolean pulling = (motorKey && whenCrouching) || (!motorKey && whenNotCrouching);
            if (!pulling) return false;
            double emberCost = ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorEmberCost;
            return EmberMotorHelper.hasEmber(player, emberCost);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isStandingOnGround(EntityPlayer player) {
        ensureInit();
        if (Reflect.isOnGround(player)) return true;
        Object controller = Reflect.getGrappleController(player);
        if (controller == null || controllerOnGroundTimerField == null) return false;
        try {
            return controllerOnGroundTimerField.getInt(controller) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static int getMode(EntityPlayer player) {
        ensureInit();
        if (isMotorPulling(player)) {
            return MODE_NEUTRAL;
        }

        double climbup = 0.0D;
        if (isKeyDown(keyClimbField)) {
            climbup = getControllerForward(player);
            if (isControllerSneaking(player)) {
                climbup = climbup / 0.3D;
            }
            if (climbup > 1.0D) climbup = 1.0D;
            else if (climbup < -1.0D) climbup = -1.0D;
        } else if (isBoundKeyDown(keyClimbUpField)) {
            climbup = 1.0D;
        } else if (isBoundKeyDown(keyClimbDownField)) {
            climbup = -1.0D;
        }

        if (climbup > 0.01D) return MODE_CLIMB;
        if (climbup < -0.01D) return MODE_DESCEND;
        if (Reflect.getSpeed(player) >= ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.grappleSwingSpeedThreshold) {
            return MODE_SWING;
        }
        return MODE_NEUTRAL;
    }

    private static double getControllerForward(EntityPlayer player) {
        Object controller = Reflect.getGrappleController(player);
        if (controller == null || controllerForwardField == null) return 0.0D;
        try {
            return controllerForwardField.getDouble(controller);
        } catch (Exception e) {
            return 0.0D;
        }
    }

    private static boolean isControllerSneaking(EntityPlayer player) {
        Object controller = Reflect.getGrappleController(player);
        if (controller != null && controllerSneakField != null) {
            try {
                return controllerSneakField.getBoolean(controller);
            } catch (Exception ignored) {}
        }
        return Reflect.isSneaking(player);
    }
}
