package com.apocollis.aqtweaks.mixin.rtg;

import com.apocollis.aqtweaks.perf.StageTimer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtg.event.EventHandlerCommon;

/**
 * Diagnostic only, no behaviour change. Feeds {@link StageTimer} (off unless {@code Performance Stage
 * Timers} is on) with how often RTG's wool-trick relight fires and what each {@code fixLightingAround}
 * costs, so the lighting work can be sized from data. Buckets in the {@code [AQ-PERF]} line:
 * {@code rtg.fixLightingAround} (count and time per call) and {@code rtg.woolRelight} (count only; the
 * time shown is 0 by design, the cost lands in the enclosing {@code rtg.fixLightingAround}).
 */
@Mixin(value = EventHandlerCommon.class, remap = false)
public abstract class MixinRtgFixLightingProbe {

    @Unique
    private static final ThreadLocal<long[]> AQTWEAKS$START = ThreadLocal.withInitial(() -> new long[1]);

    @Inject(method = "fixLightingAround(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;)V",
            at = @At("HEAD"))
    private static void aqtweaks$fixLightingAroundHead(World world, ChunkPos pos, CallbackInfo ci) {
        AQTWEAKS$START.get()[0] = StageTimer.start();
    }

    @Inject(method = "fixLightingAround(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;)V",
            at = @At("RETURN"))
    private static void aqtweaks$fixLightingAroundReturn(World world, ChunkPos pos, CallbackInfo ci) {
        long[] start = AQTWEAKS$START.get();
        StageTimer.end("rtg.fixLightingAround", start[0]);
        start[0] = 0L;
    }
}
