package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

@Mixin(value = ChunkGeneratorRTG.class, remap = false)
public abstract class MixinChunkGeneratorRTGVillage {

    @Shadow
    private MapGenVillage villageGenerator;

    /**
     * Detects active villages in RTG worlds and applies radial terrain height smoothing around village centers
     * to ensure villages generate on mostly level ground with smooth edge blending into surrounding RTG terrain.
     */
    @Inject(method = "getNewerNoise", at = @At("RETURN"))
    private void smoothRTGVillageTerrain(BiomeProvider biomeProvider, int chunkX, int chunkZ, ChunkLandscape landscape, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.depthsModule.enableRTGVillageSmoothing || villageGenerator == null || landscape == null || landscape.noise == null) {
            return;
        }

        // Search 6-chunk radius around chunkX, chunkZ for active village structure centers
        BlockPos villageCenter = null;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                BlockPos checkPos = new BlockPos((chunkX + dx) * 16 + 8, 64, (chunkZ + dz) * 16 + 8);
                if (villageGenerator.isInsideStructure(checkPos)) {
                    villageCenter = checkPos;
                    break;
                }
            }
            if (villageCenter != null) break;
        }

        if (villageCenter == null) return;

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        float radius = 96.0f; // 96-block smoothing radius
        float targetHeight = villageCenter.getY() > 0 ? (float) villageCenter.getY() : 68.0f;

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                double dist = Math.sqrt(Math.pow(worldX - villageCenter.getX(), 2) + Math.pow(worldZ - villageCenter.getZ(), 2));
                if (dist < radius) {
                    float factor = (float) (dist / radius);
                    // Smoothstep Hermite curve
                    float blend = 1.0f - (factor * factor * (3.0f - 2.0f * factor));
                    int index = localX * 16 + localZ;
                    if (index >= 0 && index < landscape.noise.length) {
                        float originalHeight = landscape.noise[index];
                        landscape.noise[index] = originalHeight * (1.0f - blend) + targetHeight * blend;
                    }
                }
            }
        }
    }
}
