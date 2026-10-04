package com.apocollis.aqtweaks.mixin.simpledifficulty;

import com.charles445.simpledifficulty.block.BlockCampfire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockCampfire.class, remap = false)
public abstract class MixinBlockCampfire {

    @Redirect(
            method = {"func_180639_a", "func_180650_b"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
                    remap = true
            )
    )
    private boolean aqtweaks$notifyNeighborsOnStateChange(World world, BlockPos pos, IBlockState newState, int flags) {
        return world.setBlockState(pos, newState, flags | 1);
    }
}
