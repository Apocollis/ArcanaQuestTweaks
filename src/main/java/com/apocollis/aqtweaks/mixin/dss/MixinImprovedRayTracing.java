package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.dynamicstealth.StealthPerf;
import com.fantasticsource.mctools.ImprovedRayTracing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamic Stealth's line-of-sight trace read {@code World.getBlockState} for every step along the ray, and each
 * call looked the chunk up again (4.3 s of a 14.6 s trace cost in a 296 s capture). Within one trace the chunk
 * is kept and read directly, which is what {@code World.getBlockState} does anyway. See {@link StealthPerf}.
 */
@Mixin(value = ImprovedRayTracing.class, remap = false)
public abstract class MixinImprovedRayTracing {

    @org.spongepowered.asm.mixin.Unique
    private static final String TRACE = "rayTraceBlocks(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;IZZ)Lnet/minecraft/util/math/RayTraceResult;";

    @Inject(method = TRACE, at = @At("HEAD"))
    private static void aqtweaks$traceBegin(World world, net.minecraft.util.math.Vec3d from, net.minecraft.util.math.Vec3d to,
                                            int iterations, boolean a, boolean b,
                                            CallbackInfoReturnable<net.minecraft.util.math.RayTraceResult> cir) {
        StealthPerf.rayBegin();
    }

    @Inject(method = TRACE, at = @At("RETURN"))
    private static void aqtweaks$traceEnd(World world, net.minecraft.util.math.Vec3d from, net.minecraft.util.math.Vec3d to,
                                          int iterations, boolean a, boolean b,
                                          CallbackInfoReturnable<net.minecraft.util.math.RayTraceResult> cir) {
        StealthPerf.rayEnd();
    }

    @Redirect(method = TRACE, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;func_180495_p(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private static IBlockState aqtweaks$cachedState(World world, BlockPos pos) {
        return StealthPerf.rayState(world, pos);
    }
}
