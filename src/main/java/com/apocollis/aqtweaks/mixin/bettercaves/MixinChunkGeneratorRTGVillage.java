package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import com.apocollis.aqtweaks.rtg.VillagePlate;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtg.world.gen.ChunkGeneratorRTG;
import rtg.world.gen.ChunkLandscape;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChunkGeneratorRTG.class, remap = false)
public abstract class MixinChunkGeneratorRTGVillage {

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

    @Inject(method = "func_185932_a", at = @At("HEAD"))
    private void aqtweaks$registerVillagesBeforeTerrain(int cx, int cz, CallbackInfoReturnable<Chunk> cir) {
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

    @Inject(method = "func_185932_a", at = @At(value = "INVOKE",
            target = "Lrtg/world/gen/ChunkGeneratorRTG;generateTerrain(Lnet/minecraft/world/chunk/ChunkPrimer;[F)V"))
    private void aqtweaks$flattenLandscapeBeforeTerrain(int cx, int cz, CallbackInfoReturnable<Chunk> cir) {
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

    @ModifyArg(method = "func_185932_a", at = @At(value = "INVOKE",
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
            VillageLandHelper.layoutVillageGrid(villageGenerator, world, cx, cz, AQTWEAKS$DUMMY_PRIMER);
        } catch (Throwable ignored) {}
    }

    /**
     * Plate dry land under buildings and land roads. Raise swamp-like water under houses with a rounded pad.
     * Never write ocean or river columns, even inside a piece box.
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

        List<VillagePlate.Record> hits = VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, falloff + xzPad);
        boolean recovered = false;
        if (hits.isEmpty()) {
            VillagePlate.rememberAll(world, villageGenerator);
            hits = VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, falloff + xzPad);
            recovered = !hits.isEmpty();
        }
        if (hits.isEmpty()) {
            List<VillagePlate.Record> startHits = VillagePlate.overlappingStartAabb(
                    seed, startX, chunkMaxX, startZ, chunkMaxZ, falloff + xzPad);
            if (!startHits.isEmpty() && VillageDebug.once("flatten:" + seed + ":" + cx + "," + cz)) {
                VillageDebug.log("flatten skip chunk=%d,%d reason=no-land-hull starts=%d",
                        cx, cz, startHits.size());
            }
            return;
        }

        List<int[]> plateBoxes = new ArrayList<>();
        List<Float> plateTargets = new ArrayList<>();
        List<int[]> raiseBoxes = new ArrayList<>();
        List<Float> raiseTargets = new ArrayList<>();
        int landBoxCount = 0;
        for (VillagePlate.Record rec : hits) {
            float target = getOrComputePlateHeight(rec);
            if (Float.isNaN(target)) continue;
            if (VillageDebug.once("plate:" + VillagePlate.key(seed, rec.xz))) {
                int[] land = VillagePlate.union(rec.landBoxesOrStart());
                VillageDebug.log("plate Y=%.1f start=[%d,%d]x[%d,%d] landBoxes=%d buildings=%d land=[%d,%d]x[%d,%d] pad=%d falloff=%d",
                        target,
                        rec.xz[0], rec.xz[1], rec.xz[2], rec.xz[3],
                        rec.landBoxesOrStart().size(), rec.buildingBoxesOrEmpty().size(),
                        land != null ? land[0] : 0, land != null ? land[1] : 0,
                        land != null ? land[2] : 0, land != null ? land[3] : 0,
                        xzPad, falloff);
            }
            for (int[] box : rec.landBoxesOrStart()) {
                plateBoxes.add(VillagePlate.padded(box, xzPad));
                plateTargets.add(target);
                landBoxCount++;
            }
            for (int[] box : rec.buildingBoxesOrEmpty()) {
                raiseBoxes.add(box);
                raiseTargets.add(target);
            }
        }
        if (plateBoxes.isEmpty() && raiseBoxes.isEmpty()) return;

        boolean[] skipWater = new boolean[noise.length];
        for (int localX = 0; localX < 16; ++localX) {
            int colX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int colZ = startZ + localZ;
                int index = localX * 16 + localZ;
                if (index < 0 || index >= noise.length) continue;
                Biome biome = Reflect.getBiome(biomeProvider, colX, colZ);
                if (VillageLandHelper.isNeverRaiseBiome(biome)) {
                    skipWater[index] = true;
                    continue;
                }
                boolean flooded = VillageLandHelper.isLandscapeWet(landscape, index);
                if (!flooded) continue;
                boolean swampRaise = VillageLandHelper.isSwampLikeForRaise(biome)
                        && aqtweaks$inRoundedPad(colX, colZ, raiseBoxes, xzPad);
                skipWater[index] = !swampRaise;
            }
        }
        int[] wetDist = aqtweaks$wetDistance(skipWater);

        int wet = 0;
        int dry = 0;
        int written = 0;
        int padded = 0;
        int raised = 0;
        for (int localX = 0; localX < 16; ++localX) {
            int colX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int colZ = startZ + localZ;
                int index = localX * 16 + localZ;
                if (index < 0 || index >= noise.length) continue;
                if (skipWater[index]) {
                    wet++;
                    continue;
                }

                float originalHeight = noise[index];
                Biome biome = Reflect.getBiome(biomeProvider, colX, colZ);
                boolean flooded = VillageLandHelper.isLandscapeWet(landscape, index);
                if (flooded && VillageLandHelper.isSwampLikeForRaise(biome)) {
                    int raiseIdx = aqtweaks$closestBox(colX, colZ, raiseBoxes);
                    if (raiseIdx < 0) {
                        wet++;
                        continue;
                    }
                    double raiseDist = distanceToBoxXZ(colX, colZ,
                            raiseBoxes.get(raiseIdx)[0], raiseBoxes.get(raiseIdx)[1],
                            raiseBoxes.get(raiseIdx)[2], raiseBoxes.get(raiseIdx)[3]);
                    if (raiseDist > xzPad) {
                        wet++;
                        continue;
                    }
                    float raiseTarget = Math.max(raiseTargets.get(raiseIdx), (float) VillageLandHelper.FLOOD_LEVEL);
                    float desired = plateHeightAt(originalHeight, raiseTarget, colX, colZ, raiseBoxes.get(raiseIdx), slope);
                    if (desired < VillageLandHelper.FLOOD_LEVEL) {
                        desired = VillageLandHelper.FLOOD_LEVEL;
                    }
                    noise[index] = desired;
                    written++;
                    raised++;
                    continue;
                }

                dry++;
                float bestBlend = 0.0F;
                float bestTarget = originalHeight;
                int[] bestBox = null;
                for (int i = 0; i < plateBoxes.size(); i++) {
                    int[] box = plateBoxes.get(i);
                    double dist = distanceToBoxXZ(colX, colZ, box[0], box[1], box[2], box[3]);
                    float blend = blendForDistance(dist, falloff);
                    if (blend > bestBlend) {
                        bestBlend = blend;
                        bestTarget = plateTargets.get(i);
                        bestBox = box;
                    }
                }
                if (bestBlend <= 0.0F || bestBox == null) continue;

                double dist = distanceToBoxXZ(colX, colZ, bestBox[0], bestBox[1], bestBox[2], bestBox[3]);
                float desired = bestTarget;
                if (dist <= 0.0) {
                    desired = plateHeightAt(originalHeight, bestTarget, colX, colZ, bestBox, slope);
                    noise[index] = desired;
                    written++;
                    padded++;
                    continue;
                }

                int bank = VillageLandHelper.BANK_BLEND;
                float waterFactor = wetDist[index] >= bank ? 1.0F : wetDist[index] / (float) bank;
                bestBlend *= waterFactor;
                if (bestBlend <= 0.0F) continue;
                noise[index] = originalHeight * (1.0F - bestBlend) + desired * bestBlend;
                written++;
            }
        }
        if (VillageDebug.once("flatten:" + seed + ":" + cx + "," + cz)) {
            if (written == 0) {
                VillageDebug.log("flatten skip chunk=%d,%d reason=written=0 landBoxes=%d dry=%d wet=%d pad=%d raised=%d recovered=%s",
                        cx, cz, landBoxCount, dry, wet, padded, raised, recovered);
            } else {
                VillageDebug.log("flatten chunk=%d,%d boxes=%d dry=%d wet=%d written=%d pad=%d raised=%d recovered=%s",
                        cx, cz, landBoxCount, dry, wet, written, padded, raised, recovered);
            }
        }
        if (landscape != null && landscape.noise != null && landscape.noise != noise) {
            System.arraycopy(noise, 0, landscape.noise, 0, Math.min(noise.length, landscape.noise.length));
        }
    }

    @Unique
    private float getOrComputePlateHeight(VillagePlate.Record rec) {
        long seed = world != null ? Reflect.getSeed(world) : 0L;
        Float cached = VillagePlate.get(seed, rec.xz);
        if (cached != null) return cached;

        BiomeProvider biomeProvider;
        try {
            biomeProvider = world.getBiomeProvider();
        } catch (Throwable t) {
            return Float.NaN;
        }
        Biome wellBiome = Reflect.getBiome(biomeProvider, rec.wellX, rec.wellZ);
        boolean swampWell = VillageLandHelper.isSwampLikeForRaise(wellBiome);

        float wellHeight = VillageLandHelper.sampleNoise((ChunkGeneratorRTG) (Object) this, biomeProvider, rec.wellX, rec.wellZ);
        String source = "well";
        if (!VillageLandHelper.isUsableHeight(wellHeight)) {
            wellHeight = aqtweaks$sampleLandFallback(biomeProvider, rec);
            source = "land";
        }

        float target;
        boolean fallback;
        if (!VillageLandHelper.isUsableHeight(wellHeight)) {
            if (swampWell) {
                target = VillageLandHelper.FLOOD_LEVEL;
                fallback = true;
                source = "swamp64";
            } else {
                if (VillageDebug.once("plateSample:" + VillagePlate.key(seed, rec.xz))) {
                    VillageDebug.log("plateSample well=%d,%d biome=%s raw=none target=skip fallback=yes",
                            rec.wellX, rec.wellZ, VillageLandHelper.biomeId(wellBiome));
                }
                return Float.NaN;
            }
        } else if (swampWell && wellHeight < VillageLandHelper.FLOOD_LEVEL) {
            target = VillageLandHelper.FLOOD_LEVEL;
            fallback = true;
        } else {
            target = wellHeight;
            fallback = !"well".equals(source);
        }
        VillagePlate.put(seed, rec.xz, target);
        if (VillageDebug.once("plateSample:" + VillagePlate.key(seed, rec.xz))) {
            VillageDebug.log("plateSample well=%d,%d biome=%s raw=%.1f target=%.1f source=%s fallback=%s",
                    rec.wellX, rec.wellZ, VillageLandHelper.biomeId(wellBiome),
                    wellHeight, target, source, fallback ? "yes" : "no");
        }
        return target;
    }

    @Unique
    private float aqtweaks$sampleLandFallback(BiomeProvider biomeProvider, VillagePlate.Record rec) {
        ChunkGeneratorRTG gen = (ChunkGeneratorRTG) (Object) this;
        for (int[] box : rec.landBoxesOrStart()) {
            int x = (box[0] + box[1]) >> 1;
            int z = (box[2] + box[3]) >> 1;
            Biome biome = Reflect.getBiome(biomeProvider, x, z);
            if (VillageLandHelper.isNeverRaiseBiome(biome)) continue;
            float height = VillageLandHelper.sampleNoise(gen, biomeProvider, x, z);
            if (VillageLandHelper.isUsableHeight(height) && height >= VillageLandHelper.FLOOD_LEVEL) {
                return height;
            }
        }
        return Float.NaN;
    }

    @Unique
    private static boolean aqtweaks$inRoundedPad(int x, int z, List<int[]> boxes, int pad) {
        for (int[] box : boxes) {
            if (distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]) <= pad) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static int aqtweaks$closestBox(int x, int z, List<int[]> boxes) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < boxes.size(); i++) {
            int[] box = boxes.get(i);
            double dist = distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    @Unique
    private static int[] aqtweaks$wetDistance(boolean[] wet) {
        int[] dist = new int[256];
        java.util.Arrays.fill(dist, 99);
        int n = Math.min(256, wet.length);
        for (int i = 0; i < n; i++) {
            if (wet[i]) dist[i] = 0;
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int i = x * 16 + z;
                if (x > 0) dist[i] = Math.min(dist[i], dist[(x - 1) * 16 + z] + 1);
                if (z > 0) dist[i] = Math.min(dist[i], dist[x * 16 + (z - 1)] + 1);
                if (x > 0 && z > 0) dist[i] = Math.min(dist[i], dist[(x - 1) * 16 + (z - 1)] + 1);
            }
        }
        for (int x = 15; x >= 0; x--) {
            for (int z = 15; z >= 0; z--) {
                int i = x * 16 + z;
                if (x < 15) dist[i] = Math.min(dist[i], dist[(x + 1) * 16 + z] + 1);
                if (z < 15) dist[i] = Math.min(dist[i], dist[x * 16 + (z + 1)] + 1);
                if (x < 15 && z < 15) dist[i] = Math.min(dist[i], dist[(x + 1) * 16 + (z + 1)] + 1);
            }
        }
        return dist;
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
