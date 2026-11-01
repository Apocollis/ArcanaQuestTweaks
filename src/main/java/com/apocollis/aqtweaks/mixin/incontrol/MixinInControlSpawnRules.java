package com.apocollis.aqtweaks.mixin.incontrol;

import com.apocollis.aqtweaks.spawning.SpawnRuleDecider;
import mcjty.incontrol.ForgeEventHandlers;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves In Control's spawn.json rules out of the spawn check and the join while Tweaks decides them. */
@Mixin(value = ForgeEventHandlers.class, remap = false)
public abstract class MixinInControlSpawnRules {

    @Inject(method = "onEntityJoinWorld", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipJoinRules(EntityJoinWorldEvent event, CallbackInfo ci) {
        if (SpawnRuleDecider.deciding()) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntitySpawnEvent", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipSpawnCheckRules(LivingSpawnEvent.CheckSpawn event, CallbackInfo ci) {
        if (SpawnRuleDecider.deciding()) {
            ci.cancel();
        }
    }
}
