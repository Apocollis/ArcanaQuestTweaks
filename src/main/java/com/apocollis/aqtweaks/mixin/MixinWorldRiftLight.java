package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.portal.RiftLighting;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorldRiftLight {

    @Inject(method = "getRawLight", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$riftLight(BlockPos pos, EnumSkyBlock type, CallbackInfoReturnable<Integer> cir) {
        if (!RiftLighting.isActive() || type != EnumSkyBlock.BLOCK) {
            return;
        }
        int extra = RiftLighting.lightAt((World) (Object) this, pos);
        if (extra > cir.getReturnValueI()) {
            cir.setReturnValue(extra);
        }
    }
}
