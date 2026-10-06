package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.world.DeferredRelight;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code World.checkLight} returns {@code false} only when the area around the position is not loaded, in
 * which case it did nothing. Hand those positions to {@link DeferredRelight} so the check is retried.
 * Lives in the early json: {@link World} is already loaded when late mixins prepare.
 */
@Mixin(World.class)
public class MixinWorldCheckLight {

    @Inject(method = "checkLight(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("RETURN"))
    private void aqtweaks$deferFailedLightCheck(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            DeferredRelight.onCheckFailed((World) (Object) this, pos);
        } else if (DeferredRelight.debugOn()) {
            DeferredRelight.onCheckSucceeded((World) (Object) this, pos);
        }
    }
}
