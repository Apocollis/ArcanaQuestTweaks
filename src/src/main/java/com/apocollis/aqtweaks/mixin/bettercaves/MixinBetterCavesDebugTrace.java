package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.yungnickyoung.minecraft.bettercaves.world.MapGenBetterCaves;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MapGenBetterCaves.class, remap = false)
public abstract class MixinBetterCavesDebugTrace {

    private static int loggedChunks = 0;

    @Inject(method = "func_186125_a", at = @At("HEAD"))
    private void onGenerateStart(World worldIn, int chunkX, int chunkZ, ChunkPrimer primer, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule) {
            if (loggedChunks < 20) {
                int dim = (worldIn != null && worldIn.provider != null) ? worldIn.provider.getDimension() : 0;
                System.out.println("[AQ-TRACE] MapGenBetterCaves.func_186125_a START for Chunk [" + chunkX + ", " + chunkZ + "] in Dim " + dim);
            }
        }
    }

    @Inject(method = "func_186125_a", at = @At("RETURN"))
    private void onGenerateEnd(World worldIn, int chunkX, int chunkZ, ChunkPrimer primer, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule) {
            if (loggedChunks < 20) {
                loggedChunks++;
                System.out.println("[AQ-TRACE] MapGenBetterCaves.func_186125_a END for Chunk [" + chunkX + ", " + chunkZ + "]");
            }
        }
    }
}
