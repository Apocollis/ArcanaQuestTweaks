package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.spawning.CageSpawnerHooks;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobSpawnerBaseLogic.class)
public abstract class MixinMobSpawnerBaseLogic {

    @Shadow
    private int spawnDelay;

    @Shadow
    public abstract World getSpawnerWorld();

    @Inject(method = "updateSpawner", at = @At("HEAD"))
    private void aqtweaks$cageDelayHead(CallbackInfo ci) {
        CageSpawnerHooks.onUpdateHead(this.spawnDelay);
    }

    @Inject(method = "updateSpawner", at = @At("RETURN"))
    private void aqtweaks$cageDelayReturn(CallbackInfo ci) {
        int failDelay = CageSpawnerHooks.failRecheckDelayOrSkip(this.spawnDelay, this.getSpawnerWorld());
        if (failDelay >= 0) {
            this.spawnDelay = failDelay;
        }
    }
}
