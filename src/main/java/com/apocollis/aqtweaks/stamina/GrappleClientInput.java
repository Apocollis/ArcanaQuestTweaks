package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.yyon.grapplinghook.ClientProxyClass;
import com.yyon.grapplinghook.GrappleCustomization;
import com.yyon.grapplinghook.controllers.grappleController;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side Grappling Hook input (climb / motor keys and controller state). Compile-hard against
 * Grappling Hook; only entered after {@link GrappleHelper#isLoaded()} succeeds, so the class is
 * never loaded without the mod.
 */
@SideOnly(Side.CLIENT)
public final class GrappleClientInput {
    public static final int MODE_NEUTRAL = PacketSyncGrappleInput.MODE_NEUTRAL;
    public static final int MODE_CLIMB = PacketSyncGrappleInput.MODE_CLIMB;
    public static final int MODE_DESCEND = PacketSyncGrappleInput.MODE_DESCEND;
    public static final int MODE_SWING = PacketSyncGrappleInput.MODE_SWING;

    private GrappleClientInput() {}

    private static boolean isKeyDown(KeyBinding key) {
        return key != null && key.isKeyDown();
    }

    /** Down, and actually bound (an unbound key reports code 0). */
    private static boolean isBoundKeyDown(KeyBinding key) {
        return key != null && key.getKeyCode() != 0 && key.isKeyDown();
    }

    public static boolean isMotorPulling(EntityPlayer player) {
        GrappleCustomization custom = GrappleHelper.customization(player);
        if (custom == null || !custom.motor) return false;
        boolean motorKey = isKeyDown(ClientProxyClass.key_motoronoff);
        boolean pulling = (motorKey && custom.motorwhencrouching) || (!motorKey && custom.motorwhennotcrouching);
        if (!pulling) return false;
        double emberCost = ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorEmberCost;
        return EmberMotorHelper.hasEmber(player, emberCost);
    }

    public static boolean isStandingOnGround(EntityPlayer player) {
        if (player.onGround) return true;
        grappleController controller = GrappleHelper.controller(player);
        return controller != null && controller.ongroundtimer > 0;
    }

    public static int getMode(EntityPlayer player) {
        if (isMotorPulling(player)) {
            return MODE_NEUTRAL;
        }

        double climbup = 0.0D;
        if (isKeyDown(ClientProxyClass.key_climb)) {
            climbup = getControllerForward(player);
            if (isControllerSneaking(player)) {
                climbup = climbup / 0.3D;
            }
            if (climbup > 1.0D) climbup = 1.0D;
            else if (climbup < -1.0D) climbup = -1.0D;
        } else if (isBoundKeyDown(ClientProxyClass.key_climbup)) {
            climbup = 1.0D;
        } else if (isBoundKeyDown(ClientProxyClass.key_climbdown)) {
            climbup = -1.0D;
        }

        if (climbup > 0.01D) return MODE_CLIMB;
        if (climbup < -0.01D) return MODE_DESCEND;
        if (Math.sqrt(player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ) >= ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.grappleSwingSpeedThreshold) {
            return MODE_SWING;
        }
        return MODE_NEUTRAL;
    }

    private static double getControllerForward(EntityPlayer player) {
        grappleController controller = GrappleHelper.controller(player);
        return controller != null ? controller.playerforward : 0.0D;
    }

    private static boolean isControllerSneaking(EntityPlayer player) {
        grappleController controller = GrappleHelper.controller(player);
        return controller != null ? controller.playersneak : player.isSneaking();
    }
}
