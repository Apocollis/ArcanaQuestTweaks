package com.apocollis.aqtweaks.mixin.bettercaves;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenCaves;

/**
 * Suppress vanilla / Depths narrow worm caves only.
 * MapGenBetterCaves extends MapGenCaves — never cancel for that subclass.
 */
@Mixin(MapGenCaves.class)
public abstract class MixinDepthsMapGenCaves {

    @Inject(method = "recursiveGenerate", remap = true, at = @At("HEAD"), cancellable = true)
    private void onRecursiveGenerateVanillaOnly(World worldIn, int chunkX, int chunkZ, int p_180701_4_, int p_180701_5_, ChunkPrimer primerIn, CallbackInfo ci) {
        // Better Caves subclasses MapGenCaves; leave its generate path alone.
        String className = this.getClass().getName();
        if ("net.minecraft.world.gen.MapGenCaves".equals(className)) {
            ci.cancel();
        }
    }
}
