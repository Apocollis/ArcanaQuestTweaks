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

    private static boolean initialized;
    private static Field keyClimbField;
    private static Field keyClimbUpField;
    private static Field keyClimbDownField;
    private static Field keyMotorField;
    private static Field customMotorField;
    private static Field customMotorWhenCrouchingField;
    private static Field customMotorWhenNotCrouchingField;
    private static Method keyIsKeyDownMethod;

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

            Object sampleKey = keyClimbField.get(null);
            if (sampleKey != null) {
                Class<?> keyClass = sampleKey.getClass();
                try {
                    keyIsKeyDownMethod = keyClass.getMethod("func_151470_d");
                } catch (NoSuchMethodException e) {
                    keyIsKeyDownMethod = keyClass.getMethod("isKeyDown");
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

    public static int getMode(EntityPlayer player) {
        ensureInit();
        if (isMotorPulling(player)) {
            return MODE_NEUTRAL;
        }

        double climbup = 0.0D;
        if (isKeyDown(keyClimbField)) {
            climbup = Reflect.getMoveForward(player);
            if (Reflect.isSneaking(player)) {
                climbup = climbup / 0.3D;
            }
            if (climbup > 1.0D) climbup = 1.0D;
            else if (climbup < -1.0D) climbup = -1.0D;
        } else if (isKeyDown(keyClimbUpField)) {
            climbup = 1.0D;
        } else if (isKeyDown(keyClimbDownField)) {
            climbup = -1.0D;
        }

        if (climbup > 0.01D) return MODE_CLIMB;
        if (climbup < -0.01D) return MODE_DESCEND;
        return MODE_NEUTRAL;
    }
}
