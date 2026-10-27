package com.apocollis.aqtweaks.mixin.rtg;

import com.apocollis.aqtweaks.world.RtgLightFixQueue;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtg.event.EventHandlerCommon;
import rtg.world.biome.BiomeProviderRTG;

/**
 * Defers and dedupes RTG's per-chunk light fix; see {@link RtgLightFixQueue}. The queue replays the original
 * method with a bypass flag set, so this hook lets that call through.
 */
@Mixin(value = EventHandlerCommon.class, remap = false)
public abstract class MixinRtgFixLightingDefer {

    @Shadow
    private static boolean alreadyFixing;

    @Inject(method = "fixLighting(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;)V",
            at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$deferFixLighting(World world, ChunkPos pos, CallbackInfo ci) {
        RtgLightFixQueue.beginColumnRepair();
        if (RtgLightFixQueue.intercept(world, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "fixLighting(Lnet/minecraft/world/World;Lnet/minecraft/util/math/ChunkPos;)V",
            at = @At("RETURN"))
    private static void aqtweaks$resyncFixedChunk(World world, ChunkPos pos, CallbackInfo ci) {
        RtgLightFixQueue.finishColumnRepair(world, pos);
    }

    @Inject(method = "fixLightingOnLoad(Lnet/minecraftforge/event/world/ChunkEvent$Load;)V",
            at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$deferFixLightingOnLoad(ChunkEvent.Load event, CallbackInfo ci) {
        RtgLightFixQueue.beginColumnRepair();
        World world = event.getWorld();
        if (world == null || world.isRemote) return;
        if (!(world.getBiomeProvider() instanceof BiomeProviderRTG)) return;
        if (alreadyFixing) return;
        Chunk chunk = event.getChunk();
        if (chunk == null) return;
        if (RtgLightFixQueue.intercept(world, chunk.getPos())) {
            ci.cancel();
        }
    }

    @Inject(method = "fixLightingOnLoad(Lnet/minecraftforge/event/world/ChunkEvent$Load;)V",
            at = @At("RETURN"))
    private static void aqtweaks$resyncLoadedChunk(ChunkEvent.Load event, CallbackInfo ci) {
        Chunk chunk = event.getChunk();
        RtgLightFixQueue.finishColumnRepair(event.getWorld(), chunk == null ? null : chunk.getPos());
    }
}
