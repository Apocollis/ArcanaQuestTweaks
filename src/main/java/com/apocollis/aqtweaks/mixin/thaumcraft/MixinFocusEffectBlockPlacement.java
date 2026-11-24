package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.battlemage.BattlemageGrief;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.common.items.casters.foci.FocusEffectFire;
import thaumcraft.common.items.casters.foci.FocusEffectFrost;

/**
 * Fire places a fire block and Frost places frost-walker ice; a mob caster must not do that to the world unless
 * {@code Allow Mob Focus Block Placement} is on (plan 2.2). The damage and the status effects are untouched.
 */
@Mixin(value = {FocusEffectFire.class, FocusEffectFrost.class}, remap = false)
public abstract class MixinFocusEffectBlockPlacement {

    /** Fire: {@code world.setBlockState(pos, fire, flags)}. */
    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
                    remap = true),
            require = 0)
    private boolean aqtweaks$fireBlock(World world, BlockPos pos, IBlockState state, int flags) {
        return BattlemageGrief.mayPlaceBlocks((FocusEffect) (Object) this) && world.setBlockState(pos, state, flags);
    }

    /** Frost: {@code world.setBlockState(pos, ice)}. */
    @Redirect(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z",
                    remap = true),
            require = 0)
    private boolean aqtweaks$frostBlock(World world, BlockPos pos, IBlockState state) {
        return BattlemageGrief.mayPlaceBlocks((FocusEffect) (Object) this) && world.setBlockState(pos, state);
    }
}
