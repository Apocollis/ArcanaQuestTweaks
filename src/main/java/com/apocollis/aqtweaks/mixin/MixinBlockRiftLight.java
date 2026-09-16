package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.portal.RiftLighting;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinBlockRiftLight {

    @Inject(
            method = "getLightValue(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private void aqtweaks$riftLightValue(IBlockState state, IBlockAccess access, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        if (!RiftLighting.isActive() || !(access instanceof World world)) {
            return;
        }
        int extra = RiftLighting.lightAt(world, pos);
        if (extra > cir.getReturnValueI()) {
            cir.setReturnValue(extra);
        }
    }
}
