package com.apocollis.aqtweaks.mixin.grapple;

import net.minecraft.client.Minecraft;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.stamina.EmberMotorHelper;


import com.yyon.grapplinghook.GrappleCustomization;
import com.yyon.grapplinghook.controllers.grappleController;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Motor Ember gating. Grappling Hook is compile-hard (see {@code GrappleHelper}); the redirect reads
 * {@code GrappleCustomization.motor} directly and no longer resolves fields reflectively on every
 * client movement tick.
 */
@Mixin(value = grappleController.class, remap = false)
public abstract class MixinGrappleController {

    @Shadow
    public Entity entity;

    @Redirect(
            method = "updatePlayerPos",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/yyon/grapplinghook/GrappleCustomization;motor:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean aqtweaks$motorRequiresEmber(GrappleCustomization custom) {
        if (!custom.motor) return false;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber) return true;

        Entity rider = this.entity;
        if (!(rider instanceof EntityPlayer)) {
            rider = Minecraft.getMinecraft().player;
        }
        if (!(rider instanceof EntityPlayer)) return true;
        return EmberMotorHelper.hasEmber((EntityPlayer) rider, ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorEmberCost);
    }
}
