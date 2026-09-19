package com.apocollis.aqtweaks.mixin.animania;

import com.animania.api.interfaces.IFoodEating;
import com.animania.common.entities.generic.ai.GenericAIMate;
import com.apocollis.aqtweaks.animania.AnimaniaModule;
import net.minecraft.entity.EntityCreature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GenericAIMate.class, remap = false)
public abstract class MixinGenericAIMate {

    @Shadow
    private EntityCreature entity;

    @Shadow
    int delayCounter;

    @Inject(method = "func_75250_a", at = @At("HEAD"))
    private void aqtweaks$rancherMateDelay(CallbackInfoReturnable<Boolean> cir) {
        if (entity == null || entity.world == null || entity.world.isRemote) return;
        if (!(entity instanceof IFoodEating eating)) return;
        if (!eating.getHandFed() && !eating.getInteracted()) return;
        if (AnimaniaModule.rancherNearby(entity)) {
            delayCounter++;
        }
    }
}
