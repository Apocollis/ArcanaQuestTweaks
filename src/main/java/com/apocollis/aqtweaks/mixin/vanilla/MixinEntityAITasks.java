package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityAITasks.class)
public class MixinEntityAITasks {

    @Redirect(
            method = "onUpdateTasks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/EntityAIBase;updateTask()V"
            )
    )
    private void aqtweaks$safeUpdateTask(EntityAIBase action) {
        try {
            action.updateTask();
        } catch (NullPointerException e) {
            try {
                action.resetTask();
            } catch (Throwable ignored) {
            }
        }
    }
}
