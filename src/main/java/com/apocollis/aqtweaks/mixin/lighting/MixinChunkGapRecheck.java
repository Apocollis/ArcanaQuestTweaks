package com.apocollis.aqtweaks.mixin.lighting;

import com.apocollis.aqtweaks.world.GapRecheckPacer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side pacing of {@code Chunk.recheckGaps}: one column per call and skipped when the shared tick slice is
 * spent. See {@link GapRecheckPacer}. Early json: {@link Chunk} is already loaded when late mixins prepare.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkGapRecheck {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private World world;

    @Inject(method = "recheckGaps(Z)V", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$paceGapRecheck(boolean onlyOne, CallbackInfo ci) {
        Chunk self = (Chunk) (Object) this;
        if (GapRecheckPacer.skip(this.world, self.x, self.z)) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "recheckGaps(Z)V", at = @At("HEAD"), argsOnly = true)
    private boolean aqtweaks$gapOneColumn(boolean onlyOne) {
        return onlyOne || GapRecheckPacer.active(this.world);
    }
}
