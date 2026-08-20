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
            VillageLandHelper.pushGenerator((ChunkGeneratorRTG) (Object) this);
            try {
                VillageLandHelper.layoutVillageGrid(villageGenerator, world, cx, cz, AQTWEAKS$DUMMY_PRIMER);
            } finally {
                VillageLandHelper.popGenerator();
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Plate land within {@code villageComponentPad} of each land component (including roads),
     * so yards between pieces share well Y. Hermite falloff beyond that pad. Never write ocean or river.
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
        int bank = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterBank);
        int componentPad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageComponentPad);
        int shrinePad = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.smallShrinePad);
        int raiseRadius = Math.max(xzPad, bank);
        int reach = componentPad + falloff;
        int startX = cx * 16;
        int startZ = cz * 16;
        int chunkMaxX = startX + 15;
        int chunkMaxZ = startZ + 15;
        long seed = Reflect.getSeed(world);

        List<VillagePlate.Record> hits = VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, reach);
        boolean recovered = false;
        if (hits.isEmpty()) {
            VillagePlate.rememberAll(world, villageGenerator);
            hits = VillagePlate.overlappingRecords(seed, startX, chunkMaxX, startZ, chunkMaxZ, reach);
            recovered = !hits.isEmpty();
        }
        if (hits.isEmpty()) {
            List<VillagePlate.Record> startHits = VillagePlate.overlappingStartAabb(
                    seed, startX, chunkMaxX, startZ, chunkMaxZ, reach);
            if (!startHits.isEmpty() && VillageDebug.once("flatten:" + seed + ":" + cx + "," + cz)) {
                VillageDebug.log("flatten skip chunk=%d,%d reason=no-land-boxes starts=%d",
                        cx, cz, startHits.size());
            }
            return;
        }

        List<int[]> plateBoxes = new ArrayList<>();
        List<Float> plateTargets = new ArrayList<>();
        List<int[]> shrineBoxes = new ArrayList<>();
        List<Float> shrineTargets = new ArrayList<>();
        List<int[]> raiseBoxes = new ArrayList<>();
        List<Float> raiseTargets = new ArrayList<>();
        int[] raisePads = new int[32];
        int raiseCount = 0;
        int landBoxCount = 0;
        for (VillagePlate.Record rec : hits) {
            float target = getOrComputePlateHeight(rec);
            if (Float.isNaN(target)) continue;
            List<int[]> land = rec.landBoxesOrStart();
            List<int[]> shrines = rec.shrineBoxesOrEmpty();
            if (VillageDebug.once("plate:" + VillagePlate.key(seed, rec.xz))) {
                VillageDebug.log("plate Y=%.1f start=[%d,%d]x[%d,%d] landBoxes=%d buildings=%d shrines=%d componentPad=%d falloff=%d bank=%d shrinePad=%d",
                        target,
                        rec.xz[0], rec.xz[1], rec.xz[2], rec.xz[3],
                        land.size(), rec.buildingBoxesOrEmpty().size(), shrines.size(),
                        componentPad, falloff, bank, shrinePad);
            }
            for (int[] box : land) {
                if (VillagePlate.containsXZBox(shrines, box)) continue;
                plateBoxes.add(box);
                plateTargets.add(target);
                landBoxCount++;
            }
            for (int[] box : shrines) {
                shrineBoxes.add(box);
                shrineTargets.add(target);
            }
            for (int[] box : rec.buildingBoxesOrEmpty()) {
                if (raiseCount >= raisePads.length) {
                    int[] grown = new int[raisePads.length * 2];
                    System.arraycopy(raisePads, 0, grown, 0, raisePads.length);
                    raisePads = grown;
                }
                raiseBoxes.add(box);
                raiseTargets.add(target);
                raisePads[raiseCount++] = VillagePlate.containsXZBox(shrines, box) ? shrinePad : raiseRadius;
            }
        }
        if (plateBoxes.isEmpty() && raiseBoxes.isEmpty() && shrineBoxes.isEmpty()) return;
        if (raiseCount < raisePads.length) {
            int[] trimmed = new int[raiseCount];
            System.arraycopy(raisePads, 0, trimmed, 0, raiseCount);
            raisePads = trimmed;
        }

        int n = Math.min(256, noise.length);
        Biome[] biomes = new Biome[n];
        boolean[] skipWater = new boolean[n];
        double[] distScratch = new double[1];
        for (int localX = 0; localX < 16; ++localX) {
            int colX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int colZ = startZ + localZ;
                int index = localX * 16 + localZ;
                if (index < 0 || index >= n) continue;
                Biome biome = Reflect.getBiome(biomeProvider, colX, colZ);
                biomes[index] = biome;
                if (VillageLandHelper.isNeverRaiseBiome(biome)) {
                    skipWater[index] = true;
                    continue;
                }
                boolean flooded = VillageLandHelper.isLandscapeWet(landscape, index);
                if (!flooded) continue;
                if (!VillageLandHelper.isSwampLikeForRaise(biome)) {
                    skipWater[index] = true;
                    continue;
                }
                int landIdx = aqtweaks$nearestBox(colX, colZ, plateBoxes, distScratch);
                boolean inPlate = landIdx >= 0 && distScratch[0] <= reach;
                boolean inShrine = aqtweaks$nearestBox(colX, colZ, shrineBoxes, distScratch) >= 0
                        && distScratch[0] <= shrinePad + falloff;
                boolean inRaise = aqtweaks$nearestPadded(colX, colZ, raiseBoxes, raisePads, distScratch) >= 0;
                skipWater[index] = !inPlate && !inShrine && !inRaise;
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
                if (index < 0 || index >= n) continue;
                if (skipWater[index]) {
                    wet++;
                    continue;
                }

                float originalHeight = noise[index];
                Biome biome = biomes[index];
                boolean flooded = VillageLandHelper.isLandscapeWet(landscape, index);
                boolean swampLike = VillageLandHelper.isSwampLikeForRaise(biome);

                int landIdx = aqtweaks$nearestBox(colX, colZ, plateBoxes, distScratch);
                double landDist = landIdx >= 0 ? distScratch[0] : Double.MAX_VALUE;
                float landBlend = landIdx >= 0 ? aqtweaks$componentBlend(landDist, componentPad, falloff) : 0.0F;
                int[] landBox = landIdx >= 0 ? plateBoxes.get(landIdx) : null;
                float landTarget = landIdx >= 0 ? plateTargets.get(landIdx) : originalHeight;

                int shrineIdx = aqtweaks$nearestBox(colX, colZ, shrineBoxes, distScratch);
                double shrineDist = shrineIdx >= 0 ? distScratch[0] : Double.MAX_VALUE;
                float shrineBlend = shrineIdx >= 0 ? aqtweaks$componentBlend(shrineDist, shrinePad, falloff) : 0.0F;
                int[] shrineBox = shrineIdx >= 0 ? shrineBoxes.get(shrineIdx) : null;
                float shrineTarget = shrineIdx >= 0 ? shrineTargets.get(shrineIdx) : originalHeight;

                float bestBlend = landBlend;
                float bestTarget = landTarget;
                int[] bestBox = landBox;
                double bestDist = landDist;
                int bestPad = componentPad;
                if (shrineBlend > bestBlend) {
                    bestBlend = shrineBlend;
                    bestTarget = shrineTarget;
                    bestBox = shrineBox;
                    bestDist = shrineDist;
                    bestPad = shrinePad;
                }

                if (flooded && swampLike) {
                    if (bestBlend >= 1.0F && bestBox != null) {
                        float core = plateHeightAt(originalHeight, bestTarget, colX, colZ, bestBox, slope);
                        if (core < VillageLandHelper.FLOOD_LEVEL) {
                            core = VillageLandHelper.FLOOD_LEVEL;
                        }
                        noise[index] = core;
                        written++;
                        raised++;
                        continue;
                    }
                    if (bestBlend > 0.0F && bestBox != null) {
                        float core = plateHeightAt(originalHeight, Math.max(bestTarget, (float) VillageLandHelper.FLOOD_LEVEL),
                                colX, colZ, bestBox, slope);
                        if (core < VillageLandHelper.FLOOD_LEVEL) {
                            core = VillageLandHelper.FLOOD_LEVEL;
                        }
                        if (bank > 0) {
                            bestBlend *= 1.0F - blendForDistance(wetDist[index], bank);
                        }
                        if (bestBlend <= 0.0F) {
                            wet++;
                            continue;
                        }
                        noise[index] = originalHeight * (1.0F - bestBlend) + core * bestBlend;
                        written++;
                        raised++;
                        continue;
                    }
                    int raiseIdx = aqtweaks$nearestPadded(colX, colZ, raiseBoxes, raisePads, distScratch);
                    if (raiseIdx < 0) {
                        wet++;
                        continue;
                    }
                    int[] raiseBox = raiseBoxes.get(raiseIdx);
                    int thisPad = raisePads[raiseIdx];
                    double raiseDist = distScratch[0];
                    float raiseTarget = Math.max(raiseTargets.get(raiseIdx), (float) VillageLandHelper.FLOOD_LEVEL);
                    float core = plateHeightAt(originalHeight, raiseTarget, colX, colZ, raiseBox, slope);
                    if (core < VillageLandHelper.FLOOD_LEVEL) {
                        core = VillageLandHelper.FLOOD_LEVEL;
                    }
                    if (raiseDist <= 0.0) {
                        noise[index] = core;
                    } else {
                        float blend = blendForDistance(raiseDist, thisPad);
                        noise[index] = originalHeight * (1.0F - blend) + core * blend;
                    }
                    written++;
                    raised++;
                    continue;
                }

                dry++;
                if (bestBlend <= 0.0F || bestBox == null) continue;
                float desired = plateHeightAt(originalHeight, bestTarget, colX, colZ, bestBox, slope);
                if (bestDist <= bestPad) {
                    noise[index] = desired;
                    written++;
                    padded++;
                    continue;
                }
                if (bank > 0) {
                    bestBlend *= 1.0F - blendForDistance(wetDist[index], bank);
                }
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
    private static int aqtweaks$nearestBox(int x, int z, List<int[]> boxes, double[] distOut) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        if (boxes == null) {
            if (distOut != null && distOut.length > 0) distOut[0] = bestDist;
            return -1;
        }
        for (int i = 0; i < boxes.size(); i++) {
            int[] box = boxes.get(i);
            double dist = distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        if (distOut != null && distOut.length > 0) distOut[0] = bestDist;
        return best;
    }

    @Unique
    private static int aqtweaks$nearestPadded(int x, int z, List<int[]> boxes, int[] pads, double[] distOut) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        int n = boxes == null || pads == null ? 0 : Math.min(boxes.size(), pads.length);
        for (int i = 0; i < n; i++) {
            int[] box = boxes.get(i);
            int pad = Math.max(0, pads[i]);
            double dist = distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]);
            if (dist > pad) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        if (distOut != null && distOut.length > 0) distOut[0] = bestDist;
        return best;
    }

    @Unique
    private static float aqtweaks$componentBlend(double dist, int pad, int falloff) {
        if (dist <= pad) return 1.0F;
        return blendForDistance(dist - pad, falloff);
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
