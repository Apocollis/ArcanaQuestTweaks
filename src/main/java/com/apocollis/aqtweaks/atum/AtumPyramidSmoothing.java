package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.PrimerAccess;
import com.teammetallurgy.atum.init.AtumBlocks;
import com.teammetallurgy.atum.world.gen.structure.pyramid.MapGenPyramid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.List;

/**
 * Flattens new Atum chunks around a registered pyramid plate and clears the entrance walk.
 * Runs after the pyramid start exists and before the chunk is built from the primer.
 * The pyramid itself is pasted later, into this terrain.
 */
public final class AtumPyramidSmoothing {

    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    private AtumPyramidSmoothing() {}

    public static void apply(World world, ChunkPrimer primer, int chunkX, int chunkZ, MapGenPyramid pyramid) {
        if (world == null || primer == null || pyramid == null) return;
        if (!ArcanaQuestTweaksConfig.AtumModuleConfig.enablePyramidTerrainSmoothing) return;
        adoptNearby(world, pyramid, chunkX, chunkZ);
        int dimension = PrimerAccess.dimensionOf(world);
        List<AtumPyramidPlate> plates = AtumPyramidPlate.overlapping(dimension, chunkX, chunkZ);
        if (plates.isEmpty()) return;
        IBlockState sand = AtumBlocks.SAND.getDefaultState();
        IBlockState stone = AtumBlocks.LIMESTONE.getDefaultState();
        if (sand == null || stone == null) return;
        int apron = ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidApronBlocks;
        int falloff = ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidFalloffBlocks;
        boolean corridor = ArcanaQuestTweaksConfig.AtumModuleConfig.enablePyramidEntranceCorridor;
        int corridorLength = ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidEntranceCorridorLength;
        int doorLift = AtumPyramidPlate.doorLift();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx;
                int z = baseZ + lz;
                for (AtumPyramidPlate plate : plates) {
                    if (plate.contains(x, z)) {
                        int natural = surfaceY(primer, lx, lz);
                        for (int y = natural; y > plate.plateY; y--) {
                            PrimerAccess.setBlockState(primer, lx, y, lz, AIR);
                        }
                        break;
                    }
                    int walked = corridor ? plate.corridorDistance(x, z, corridorLength) : 0;
                    if (walked > 0) {
                        int natural = surfaceY(primer, lx, lz);
                        int target = sloped(plate.plateY + doorLift, natural, walked, corridorLength);
                        reshape(primer, lx, lz, natural, target, sand, stone);
                        break;
                    }
                    double distance = plate.distanceOutside(x, z);
                    double reach = apron + (double) falloff;
                    if (distance > reach) continue;
                    int natural = surfaceY(primer, lx, lz);
                    int target = plateau(plate.plateY, natural, distance, apron, falloff);
                    reshape(primer, lx, lz, natural, target, sand, stone);
                    break;
                }
            }
        }
    }

    private static void adoptNearby(World world, MapGenPyramid pyramid, int chunkX, int chunkZ) {
        it.unimi.dsi.fastutil.longs.Long2ObjectMap<StructureStart> starts =
                ((com.apocollis.aqtweaks.mixin.vanilla.AccessorMapGenStructure) pyramid).aqtweaks$getStructureMap();
        if (starts == null || starts.isEmpty()) return;
        // A plate reshapes terrain up to this many chunks from its start (5 with the default margins). Starts that
        // never went through create (saved structure data) only reach the plate registry through adopt, so the
        // scan has to cover the full reach (plan 20261027-1527).
        int reach = AtumPyramidPlate.reachChunks();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                StructureStart start = starts.get(net.minecraft.util.math.ChunkPos.asLong(chunkX + dx, chunkZ + dz));
                if (start != null && AtumPyramidPlate.adopt(world, start) && Math.max(Math.abs(dx), Math.abs(dz)) > 4) {
                    org.apache.logging.log4j.LogManager.getLogger("AQTweaks-Atum").debug(
                            "[AQ-VERIFY] atum late plate adopt chunk={},{} start={},{}", chunkX, chunkZ, chunkX + dx, chunkZ + dz);
                }
            }
        }
    }

    private static int plateau(int plateY, int natural, double distance, int apron, int falloff) {
        if (distance <= apron || falloff <= 0) return plateY;
        double t = (distance - apron) / falloff;
        if (t < 0.0D) t = 0.0D;
        if (t > 1.0D) t = 1.0D;
        double hermite = t * t * (3.0D - 2.0D * t);
        return (int) Math.round(plateY * (1.0D - hermite) + natural * hermite);
    }

    private static int sloped(int plateY, int natural, int distance, int length) {
        if (length <= 1) return natural;
        double t = (distance - 1) / (double) (length - 1);
        if (t < 0.0D) t = 0.0D;
        if (t > 1.0D) t = 1.0D;
        return (int) Math.round(plateY * (1.0D - t) + natural * t);
    }

    private static void reshape(ChunkPrimer primer, int lx, int lz, int natural, int target,
                                IBlockState sand, IBlockState stone) {
        if (target < 2) target = 2;
        if (target > 250) target = 250;
        int top = natural > 0 ? natural : target;
        for (int y = top; y > target; y--) {
            PrimerAccess.setBlockState(primer, lx, y, lz, AIR);
        }
        if (natural > 0 && natural < target) {
            for (int y = natural + 1; y < target; y++) {
                PrimerAccess.setBlockState(primer, lx, y, lz, stone);
            }
        }
        PrimerAccess.setBlockState(primer, lx, target, lz, sand);
        if (target - 1 >= 1) PrimerAccess.setBlockState(primer, lx, target - 1, lz, stone);
        if (target - 2 >= 1) PrimerAccess.setBlockState(primer, lx, target - 2, lz, stone);
    }

    private static int surfaceY(ChunkPrimer primer, int lx, int lz) {
        for (int y = 255; y >= 1; y--) {
            IBlockState state = PrimerAccess.getBlockState(primer, lx, y, lz);
            Material material = PrimerAccess.getMaterial(state);
            if (material == Material.AIR || material == Material.PLANTS || material == Material.VINE
                    || material == Material.SNOW || material == Material.LEAVES || material == Material.WATER) {
                continue;
            }
            return y;
        }
        return 0;
    }
}
