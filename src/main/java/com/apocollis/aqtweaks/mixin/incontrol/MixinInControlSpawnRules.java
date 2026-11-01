package com.apocollis.aqtweaks.mixin.incontrol;

import com.apocollis.aqtweaks.spawning.ConditionShadow;
import com.apocollis.aqtweaks.spawning.RuleShadow;
import com.apocollis.aqtweaks.spawning.SpawnRuleDecider;
import mcjty.incontrol.ForgeEventHandlers;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves In Control's spawn.json rules out of the spawn check and the join while Tweaks decides them, and otherwise marks
 * the moments just before and just after In Control's own handlers, so the shadow comparison blames In Control only for
 * what In Control did (not for what the game or other mods did to the mob or the event beforehand).
 */
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

    @Inject(method = "onEntityJoinWorld", at = @At("HEAD"))
    private void aqtweaks$beforeJoinRules(EntityJoinWorldEvent event, CallbackInfo ci) {
        RuleShadow.beforeInControlJoin(event);
    }

    @Inject(method = "onEntityJoinWorld", at = @At("RETURN"))
    private void aqtweaks$afterJoinRules(EntityJoinWorldEvent event, CallbackInfo ci) {
        ConditionShadow.afterInControlJoin(event);
        RuleShadow.afterInControlJoin(event);
    }

    @Inject(method = "onEntitySpawnEvent", at = @At("HEAD"))
    private void aqtweaks$beforeSpawnCheckRules(LivingSpawnEvent.CheckSpawn event, CallbackInfo ci) {
        ConditionShadow.beforeInControlCheck(event);
    }

    @Inject(method = "onEntitySpawnEvent", at = @At("RETURN"))
    private void aqtweaks$afterSpawnCheckRules(LivingSpawnEvent.CheckSpawn event, CallbackInfo ci) {
        ConditionShadow.afterInControlCheck(event);
    }
}
