package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChunkGeneratorRTG.class, remap = false)
public abstract class MixinChunkGeneratorRTGVillage {

    @Shadow
    private MapGenVillage villageGenerator;

    /**
     * Flatten RTG height inside each MapGenVillage AABB, then smoothstep blend by distance to the box edge.
     */
    @Inject(method = "getNewerNoise", at = @At("RETURN"))
    private void smoothRTGVillageTerrain(BiomeProvider biomeProvider, int chunkX, int chunkZ, ChunkLandscape landscape, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageSmoothing
                || villageGenerator == null || landscape == null || landscape.noise == null) {
            return;
        }

        int falloff = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageEdgeFalloff);
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int chunkMaxX = startX + 15;
        int chunkMaxZ = startZ + 15;

        List<int[]> boxes = new ArrayList<>();
        for (Object start : Reflect.getMapGenStructureStarts(villageGenerator)) {
            int[] box = Reflect.getStructureStartBoxXZ(start);
            if (box == null) continue;
            int minX = box[0];
            int maxX = box[1];
            int minZ = box[2];
            int maxZ = box[3];
            if (maxX + falloff < startX || minX - falloff > chunkMaxX) continue;
            if (maxZ + falloff < startZ || minZ - falloff > chunkMaxZ) continue;
            boxes.add(box);
        }
        if (boxes.isEmpty()) return;

        float[] targets = new float[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            int[] box = boxes.get(i);
            float sum = 0.0F;
            int count = 0;
            for (int localX = 0; localX < 16; ++localX) {
                int worldX = startX + localX;
                if (worldX < box[0] || worldX > box[1]) continue;
                for (int localZ = 0; localZ < 16; ++localZ) {
                    int worldZ = startZ + localZ;
                    if (worldZ < box[2] || worldZ > box[3]) continue;
                    int index = localX * 16 + localZ;
                    if (index >= 0 && index < landscape.noise.length) {
                        sum += landscape.noise[index];
                        count++;
                    }
                }
            }
            if (count > 0) {
                targets[i] = sum / count;
            } else {
                float chunkSum = 0.0F;
                int chunkCount = 0;
                for (int n = 0; n < landscape.noise.length; n++) {
                    chunkSum += landscape.noise[n];
                    chunkCount++;
                }
                targets[i] = chunkCount > 0 ? chunkSum / chunkCount : 68.0F;
            }
        }

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                int index = localX * 16 + localZ;
                if (index < 0 || index >= landscape.noise.length) continue;

                float originalHeight = landscape.noise[index];
                float bestBlend = 0.0F;
                float bestTarget = originalHeight;
                for (int i = 0; i < boxes.size(); i++) {
                    int[] box = boxes.get(i);
                    double dist = distanceToBoxXZ(worldX, worldZ, box[0], box[1], box[2], box[3]);
                    float blend;
                    if (dist <= 0.0) {
                        blend = 1.0F;
                    } else if (falloff <= 0) {
                        blend = 0.0F;
                    } else if (dist >= falloff) {
                        blend = 0.0F;
                    } else {
                        float factor = (float) (dist / falloff);
                        blend = 1.0F - (factor * factor * (3.0F - 2.0F * factor));
                    }
                    if (blend > bestBlend) {
                        bestBlend = blend;
                        bestTarget = targets[i];
                    }
                }
                if (bestBlend > 0.0F) {
                    landscape.noise[index] = originalHeight * (1.0F - bestBlend) + bestTarget * bestBlend;
                }
            }
        }
    }

    private static double distanceToBoxXZ(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        int dx = 0;
        if (x < minX) dx = minX - x;
        else if (x > maxX) dx = x - maxX;
        int dz = 0;
        if (z < minZ) dz = minZ - z;
        else if (z > maxZ) dz = z - maxZ;
        if (dx == 0 && dz == 0) return 0.0;
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }
}
