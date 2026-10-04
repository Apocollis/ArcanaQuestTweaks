package com.apocollis.aqtweaks.mixin.simpledifficulty;

import net.minecraft.block.material.Material;
import com.charles445.simpledifficulty.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a boat from reading as wet.
 *
 * <p>{@code getSidedBlockPos} floors {@code posY + 0.5} on both sides. A player riding a boat sits at
 * {@code boat.posY - 0.45} ({@code EntityBoat.getMountedYOffset()} is -0.1, {@code EntityPlayer.getYOffset()} is
 * -0.35), so that block is the water under the hull. This pack's {@code fluidTemperatures.json} has no
 * {@code "water"} entry, so {@code ModifierWet} falls through to {@code Material.WATER} and returns
 * {@code wetValue} (-6).
 *
 * <p>The lift moves <em>every</em> sample one block up while riding, not only wetness: altitude, nearby blocks,
 * rain, and the shelter test all run at the returned position. Rain is still {@code isRainingAt} on that
 * position, so an open boat in the rain stays wet.
 */
@Mixin(value = WorldUtil.class, remap = false)
public abstract class MixinWorldUtil {

    @Inject(method = "getSidedBlockPos", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$boatLift(World world, Entity entity, CallbackInfoReturnable<BlockPos> cir) {
        if (world == null || !(entity instanceof EntityPlayer)) return;
        if (!(entity.getRidingEntity() instanceof EntityBoat)) return;

        BlockPos pos = cir.getReturnValue();
        if (pos == null) return;

        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() != Material.WATER) return;

        BlockPos up = pos.up();
        IBlockState upState = world.getBlockState(up);
        // Submerged boat keeps the original position.
        if (upState.getMaterial() == Material.WATER) return;
        // The lifted block must be passable, or a boat in a one-high channel would sample inside the ceiling.
        if (upState.getCollisionBoundingBox(world, up) != Block.NULL_AABB) return;

        cir.setReturnValue(up);
    }
}
