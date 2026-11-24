package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.OverchannelHooks;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;

/**
 * Ley-Rupture overchannel: every focus cast runs through {@code castFocusPackage}, which first copies the package. The
 * copy is the one we may scale, so the stored focus is never touched (plan 6.13). The hook decides whether the caster
 * is overchanneling and charges its cost.
 */
@Mixin(value = FocusEngine.class, remap = false)
public abstract class MixinFocusEngineOverchannel {

    @Redirect(
            method = "castFocusPackage(Lnet/minecraft/entity/EntityLivingBase;Lthaumcraft/api/casters/FocusPackage;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lthaumcraft/api/casters/FocusPackage;copy(Lnet/minecraft/entity/EntityLivingBase;)Lthaumcraft/api/casters/FocusPackage;",
                    remap = false))
    private static FocusPackage aqtweaks$overchannelCopy(FocusPackage original, EntityLivingBase caster) {
        FocusPackage copy = original.copy(caster);
        OverchannelHooks.onCast(caster, copy);
        return copy;
    }
}
