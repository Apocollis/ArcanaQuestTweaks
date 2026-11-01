package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftFocusHooks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.items.casters.foci.FocusEffectHeal;

/**
 * Marks the heal-focus cast. Does not touch {@code EntityLivingBase.heal}, which Thaumic Tweaker
 * already redirects.
 */
@Mixin(value = FocusEffectHeal.class, remap = false)
public abstract class MixinFocusEffectHeal {

    @Inject(method = "execute", at = @At("HEAD"))
    private void aqtweaks$beginHeal(RayTraceResult trace, Trajectory trajectory, float power, int num,
                                    CallbackInfoReturnable<Boolean> ci) {
        Entity caster = null;
        FocusPackage pack = ((FocusEffectHeal) (Object) this).getPackage();
        if (pack != null) {
            caster = pack.getCaster();
        }
        EntityLivingBase living = null;
        if (trace != null && trace.entityHit instanceof EntityLivingBase) {
            EntityLivingBase hit = (EntityLivingBase) trace.entityHit;
            if (!hit.isEntityUndead()) {
                living = hit;
            }
        }
        ThaumcraftFocusHooks.beginHeal(caster, living);
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void aqtweaks$endHeal(RayTraceResult trace, Trajectory trajectory, float power, int num,
                                  CallbackInfoReturnable<Boolean> ci) {
        ThaumcraftFocusHooks.endHeal();
    }
}
