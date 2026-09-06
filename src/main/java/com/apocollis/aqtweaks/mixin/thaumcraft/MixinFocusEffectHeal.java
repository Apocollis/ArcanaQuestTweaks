package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftFocusHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.items.casters.foci.FocusEffectHeal;

@Mixin(value = FocusEffectHeal.class, remap = false)
public abstract class MixinFocusEffectHeal {

    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;heal(F)V",
                    remap = true
            )
    )
    private void aqtweaks$scaleFocusHeal(EntityLivingBase target, float amount) {
        Entity caster = null;
        FocusPackage pack = ((FocusEffectHeal) (Object) this).getPackage();
        if (pack != null) {
            caster = pack.getCaster();
        }
        ThaumcraftFocusHooks.healScaled(target, caster, amount);
    }
}
