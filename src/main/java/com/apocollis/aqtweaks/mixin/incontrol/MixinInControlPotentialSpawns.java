package com.apocollis.aqtweaks.mixin.incontrol;

import com.apocollis.aqtweaks.spawning.PotentialSpawnIndex;
import mcjty.incontrol.ForgeEventHandlers;
import net.minecraftforge.event.world.WorldEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs In Control's potential-spawn rules through {@link PotentialSpawnIndex} instead of all of them. */
@Mixin(value = ForgeEventHandlers.class, remap = false)
public abstract class MixinInControlPotentialSpawns {

    @Inject(method = "onPotentialSpawns", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$indexedPotentialSpawns(WorldEvent.PotentialSpawns event, CallbackInfo ci) {
        if (PotentialSpawnIndex.handle(event)) {
            ci.cancel();
        }
    }
}
