package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.rtg.VillagePlate;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = ChunkGeneratorRTG.class, remap = false)
public abstract class MixinChunkGeneratorRTGVillage {

    @Unique
    private static final Logger AQTWEAKS$LOG = LogManager.getLogger("AQTweaks-Village");
    @Unique
    private static final Set<String> AQTWEAKS$LOGGED_PLATES = ConcurrentHashMap.newKeySet();
    @Unique
    private static final ChunkPrimer AQTWEAKS$DUMMY_PRIMER = new ChunkPrimer();

    @Unique
    private int aqtweaks$flattenCx;
    @Unique
    private int aqtweaks$flattenCz;

    @Shadow
    private MapGenVillage villageGenerator;

    @Shadow
    private World world;

    @Shadow
    public abstract ChunkLandscape getLandscape(BiomeProvider biomeProvider, ChunkPos chunkPos);

    @Inject(method = "generateChunk", at = @At("HEAD"))
    private void aqtweaks$registerVillagesBeforeTerrain(int cx, int cz, CallbackInfo ci) {
        aqtweaks$flattenCx = cx;
        aqtweaks$flattenCz = cz;
        aqtweaks$registerVillages(cx, cz);
    }

    @Inject(method = "getNewerNoise", at = @At("HEAD"))
    private void aqtweaks$registerVillagesBeforeNoise(BiomeProvider biomeProvider, int worldX, int worldZ, ChunkLandscape landscape, CallbackInfo ci) {
        if (VillageLandHelper.isSamplingLandscape()) {
            return;
        }
        aqtweaks$registerVillages(worldX >> 4, worldZ >> 4);
    }

    @Inject(method = "generateChunk", at = @At(value = "INVOKE",
            target = "Lrtg/world/gen/ChunkGeneratorRTG;generateTerrain(Lnet/minecraft/world/chunk/ChunkPrimer;[F)V"))
    private void aqtweaks$flattenLandscapeBeforeTerrain(int cx, int cz, CallbackInfo ci) {
        BiomeProvider biomeProvider;
        try {
            biomeProvider = world.getBiomeProvider();
        } catch (Throwable t) {
            return;
        }
        if (biomeProvider == null) return;
        ChunkLandscape landscape;
        try {
            landscape = getLandscape(biomeProvider, new ChunkPos(cx, cz));
        } catch (Throwable t) {
            return;
        }
        if (landscape != null && landscape.noise != null) {
            aqtweaks$flattenNoise(cx, cz, landscape.noise, landscape);
        }
    }

    @ModifyArg(method = "generateChunk", at = @At(value = "INVOKE",
            target = "Lrtg/world/gen/ChunkGeneratorRTG;generateTerrain(Lnet/minecraft/world/chunk/ChunkPrimer;[F)V"),
            index = 1)
    private float[] aqtweaks$flattenVillagePlate(float[] noise) {
        aqtweaks$flattenNoise(aqtweaks$flattenCx, aqtweaks$flattenCz, noise, null);
        return noise;
    }

    @Unique
    private void aqtweaks$registerVillages(int cx, int cz) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageSmoothing
                || villageGenerator == null || world == null) {
            return;
        }
        try {
            if (world.getWorldInfo() != null && !world.getWorldInfo().isMapFeaturesEnabled()) return;
            villageGenerator.generate(world, cx, cz, AQTWEAKS$DUMMY_PRIMER);
        } catch (Throwable ignored) {}
    }

    /**
     * Level dry columns in each village AABB to one plate height, then blend the rim into raw RTG.
     * Wet columns are left alone.
     */
    @Unique
    private void aqtweaks$flattenNoise(int cx, int cz, float[] noise, ChunkLandscape landscape) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageSmoothing
                || villageGenerator == null || world == null || noise == null) {
            return;
        }
        BiomeProvider biomeProvider;
        try {
            biomeProvider = world.getBiomeProvider();
        } catch (Throwable t) {
            return;
        }
        if (biomeProvider == null) return;

        if (landscape == null) {
            try {
                landscape = getLandscape(biomeProvider, new ChunkPos(cx, cz));
            } catch (Throwable t) {
                landscape = null;
            }
        }

        int falloff = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageEdgeFalloff);
        int slope = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villagePlateSlopeBlocks);
        int xzPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageBoxXZPad);
        int startX = cx * 16;
        int startZ = cz * 16;
        int chunkMaxX = startX + 15;
        int chunkMaxZ = startZ + 15;
        long seed = Reflect.getSeed(world);

        List<int[]> boxes = VillagePlate.overlappingXZ(seed, startX, chunkMaxX, startZ, chunkMaxZ, falloff + xzPad);
        if (boxes.isEmpty()) {
            VillagePlate.rememberAll(world, villageGenerator);
            boxes = VillagePlate.overlappingXZ(seed, startX, chunkMaxX, startZ, chunkMaxZ, falloff + xzPad);
        }
        if (boxes.isEmpty()) return;

        List<int[]> paddedBoxes = new ArrayList<>();
        for (int[] box : boxes) {
            paddedBoxes.add(VillagePlate.padded(box, xzPad));
        }

        boolean[] waterColumn = new boolean[noise.length];
        for (int localX = 0; localX < 16; ++localX) {
            int colX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int colZ = startZ + localZ;
                int index = localX * 16 + localZ;
                if (index < 0 || index >= noise.length) continue;
                waterColumn[index] = VillageLandHelper.isWetColumn(biomeProvider, landscape, index, colX, colZ);
            }
        }

        float[] targets = new float[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            targets[i] = getOrComputePlateHeight(biomeProvider, boxes.get(i));
            if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageFlattenDebug) {
                int[] box = boxes.get(i);
                String id = VillagePlate.key(seed, box);
                if (AQTWEAKS$LOGGED_PLATES.add(id)) {
                    AQTWEAKS$LOG.info("Village plate Y={} box=[{},{}]x[{},{}] pad={} falloff={}",
                            targets[i], box[0], box[1], box[2], box[3], xzPad, falloff);
                }
            }
        }

        for (int localX = 0; localX < 16; ++localX) {
            int colX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int colZ = startZ + localZ;
                int index = localX * 16 + localZ;
                if (index < 0 || index >= noise.length) continue;
                if (waterColumn[index]) continue;

                float originalHeight = noise[index];
                float bestBlend = 0.0F;
                float bestTarget = originalHeight;
                int[] bestBox = null;
                for (int i = 0; i < boxes.size(); i++) {
                    int[] padded = paddedBoxes.get(i);
                    double dist = distanceToBoxXZ(colX, colZ, padded[0], padded[1], padded[2], padded[3]);
                    float blend = blendForDistance(dist, falloff);
                    if (blend > bestBlend) {
                        bestBlend = blend;
                        bestTarget = targets[i];
                        bestBox = padded;
                    }
                }
                if (bestBlend <= 0.0F || bestBox == null) continue;

                double dist = distanceToBoxXZ(colX, colZ, bestBox[0], bestBox[1], bestBox[2], bestBox[3]);
                float desired = bestTarget;
                if (dist <= 0.0) {
                    desired = plateHeightAt(originalHeight, bestTarget, colX, colZ, bestBox, slope);
                }
                noise[index] = originalHeight * (1.0F - bestBlend) + desired * bestBlend;
            }
        }
        if (landscape != null && landscape.noise != null && landscape.noise != noise) {
            System.arraycopy(noise, 0, landscape.noise, 0, Math.min(noise.length, landscape.noise.length));
        }
    }

    @Unique
    private float getOrComputePlateHeight(BiomeProvider biomeProvider, int[] box) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        Float cached = VillagePlate.get(seed, box);
        if (cached != null) return cached;

        float sum = 0.0F;
        int count = 0;
        int minChunkX = box[0] >> 4;
        int maxChunkX = box[1] >> 4;
        int minChunkZ = box[2] >> 4;
        int maxChunkZ = box[3] >> 4;
        for (int ccx = minChunkX; ccx <= maxChunkX; ++ccx) {
            for (int ccz = minChunkZ; ccz <= maxChunkZ; ++ccz) {
                ChunkLandscape sample;
                try {
                    sample = getLandscape(biomeProvider, new ChunkPos(ccx, ccz));
                } catch (Throwable t) {
                    continue;
                }
                if (sample == null || sample.noise == null) continue;
                int originX = ccx * 16;
                int originZ = ccz * 16;
                for (int localX = 0; localX < 16; ++localX) {
                    int colX = originX + localX;
                    if (colX < box[0] || colX > box[1]) continue;
                    for (int localZ = 0; localZ < 16; ++localZ) {
                        int colZ = originZ + localZ;
                        if (colZ < box[2] || colZ > box[3]) continue;
                        int index = localX * 16 + localZ;
                        if (index < 0 || index >= sample.noise.length) continue;
                        if (VillageLandHelper.isWetColumn(biomeProvider, sample, index, colX, colZ)) continue;
                        sum += sample.noise[index];
                        count++;
                    }
                }
            }
        }
        float target = count > 0 ? sum / count : 68.0F;
        VillagePlate.put(seed, box, target);
        return target;
    }

    @Unique
    private static float plateHeightAt(float original, float target, int x, int z, int[] box, int slopeBlocks) {
        if (slopeBlocks <= 0) return target;
        double centerX = (box[0] + box[1]) * 0.5;
        double centerZ = (box[2] + box[3]) * 0.5;
        double dist = Math.hypot(x - centerX, z - centerZ);
        float maxDev = (float) (dist / (double) slopeBlocks);
        if (original > target + maxDev) return target + maxDev;
        if (original < target - maxDev) return target - maxDev;
        return original;
    }

    @Unique
    private static float blendForDistance(double dist, int falloff) {
        if (dist <= 0.0) return 1.0F;
        if (falloff <= 0) return 0.0F;
        if (dist >= falloff) return 0.0F;
        float factor = (float) (dist / falloff);
        return 1.0F - (factor * factor * (3.0F - 2.0F * factor));
    }

    @Unique
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
