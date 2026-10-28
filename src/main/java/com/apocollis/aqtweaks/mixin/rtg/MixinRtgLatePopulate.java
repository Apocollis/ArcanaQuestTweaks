package com.apocollis.aqtweaks.mixin.rtg;

import com.apocollis.aqtweaks.world.PopulateRelight;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RTG-Plus populates through {@code LatePopulatingChunk.doPopulate}, which calls {@code Chunk.checkLight()},
 * {@code IChunkGenerator.populate}, {@code GameRegistry.generateWorld} and {@code markDirty()} itself and never reaches
 * {@code Chunk.populate(IChunkGenerator)}, so {@link PopulateRelight} would not see it. This gives it the same scope.
 * The opening {@code checkLight()} is the first call in {@code doPopulate}, so the scope begins before it, as in
 * {@code MixinChunkPopulateRelight}. RTG runs queued chunks one after another, not nested; an exception inside
 * {@code doPopulate} leaves the scope for the stale check in {@link PopulateRelight#begin}.
 */
@Mixin(targets = "rtg.world.gen.LatePopulatingChunk", remap = false)
public abstract class MixinRtgLatePopulate {

    @Inject(method = "doPopulate(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/IChunkGenerator;)V",
            at = @At("HEAD"), remap = false)
    private static void aqtweaks$populateBegin(Chunk chunk, IChunkGenerator generator, CallbackInfo ci) {
        PopulateRelight.beginRtg(chunk.getWorld(), chunk);
    }

    @Inject(method = "doPopulate(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/IChunkGenerator;)V",
            at = @At("RETURN"), remap = false)
    private static void aqtweaks$populateEnd(Chunk chunk, IChunkGenerator generator, CallbackInfo ci) {
        PopulateRelight.end(chunk);
    }
}
