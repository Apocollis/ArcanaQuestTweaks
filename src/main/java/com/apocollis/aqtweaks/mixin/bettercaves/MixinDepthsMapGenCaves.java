package com.apocollis.aqtweaks.mixin.bettercaves;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenCaves;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapGenCaves.class)
public abstract class MixinDepthsMapGenCaves {

    /**
     * Suppress Depths Update / Vanilla narrow diagonal worm cave generation
     * so it doesn't carve worm tunnels through negative Y terrain.
     * (YUNG's Better Caves uses MapGenBetterCaves, which is unaffected).
     */
    @Inject(method = "recursiveGenerate", remap = true, at = @At("HEAD"), cancellable = true)
    private void onRecursiveGenerateBetterCaves(World worldIn, int chunkX, int chunkZ, int p_180701_4_, int p_180701_5_, ChunkPrimer primerIn, CallbackInfo ci) {
        ci.cancel();
    }
}
