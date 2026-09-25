package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.entity.monster.EntityGhast;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.monster.EntityGhast$AIFireballAttack")
public class MixinEntityGhastAIFireballAttack {

    @Shadow
    @Final
    private EntityGhast parentEntity;

    @Inject(method = "updateTask", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$cancelWhenTargetNull(CallbackInfo ci) {
        if (this.parentEntity.getAttackTarget() == null) {
            this.parentEntity.setAttacking(false);
            ci.cancel();
        }
    }
}
