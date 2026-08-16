package com.apocollis.aqtweaks.mixin.grapple;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.stamina.EmberMotorHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.yyon.grapplinghook.controllers.grappleController", remap = false)
public abstract class MixinGrappleController {

    @Redirect(
            method = "updatePlayerPos",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/yyon/grapplinghook/GrappleCustomization;motor:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean aqtweaks$motorRequiresEmber(Object custom) {
        boolean motor = false;
        try {
            motor = custom.getClass().getField("motor").getBoolean(custom);
        } catch (Exception ignored) {}
        if (!motor) return false;
        Entity entity = null;
        try {
            entity = (Entity) this.getClass().getField("entity").get(this);
        } catch (Exception ignored) {}
        if (!(entity instanceof EntityPlayer)) return true;
        EntityPlayer player = (EntityPlayer) entity;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber) return true;
        return EmberMotorHelper.hasEmber(player, ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorEmberCost);
    }
}
