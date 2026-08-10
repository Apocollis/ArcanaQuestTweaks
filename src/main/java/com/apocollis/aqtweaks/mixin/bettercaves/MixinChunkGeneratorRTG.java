package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "rtg.world.gen.ChunkGeneratorRTG", remap = false)
public abstract class MixinChunkGeneratorRTG {

    /**
     * Injects into RTG's generateTerrain(ChunkPrimer, float[]).
     *
     * RTG's terrain generator only generates terrain for Y = 0 to 255, leaving Y = -64 to Y = 0
     * as empty void/air. Depths Update only mixined standard ChunkGeneratorOverworld, so RTG worlds
     * lacked Deepslate generation below Y = 0.
     *
     * This mixin fills Y = minWorldY (-64) to Y = 0 with solid Deepslate (and Bedrock at Y = -64)
     * inside the ChunkPrimer right after RTG terrain density generation, providing solid terrain to carve into.
     */
    @Inject(method = "generateTerrain", at = @At("TAIL"))
    private void onGenerateTerrain(ChunkPrimer primer, float[] noise, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule) {
            int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
            if (minY < 0 && primer != null) {
                IBlockState deepslateState = Reflect.getDeepslateState();
                IBlockState bedrockState = Reflect.getBedrockState();

                for (int x = 0; x < 16; ++x) {
                    for (int z = 0; z < 16; ++z) {
                        // Bedrock floor at minY (-64)
                        if (bedrockState != null) {
                            Reflect.setBlockState(primer, x, minY, z, bedrockState);
                        }

                        // Fill solid Deepslate from minY + 1 to 0
                        if (deepslateState != null) {
                            for (int y = minY + 1; y <= 0; ++y) {
                                Reflect.setBlockState(primer, x, y, z, deepslateState);
                            }
                        }
                    }
                }
            }
        }
    }
}
