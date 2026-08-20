package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Fill under a placed schematic and ramp the rim into surrounding land. Never writes ocean/river.
 */
public final class StructureLandSettle {

    private StructureLandSettle() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableStructureLandSettle;
    }

    public static void settleAabb(World world, int minX, int maxX, int minZ, int maxZ, int floorY) {
        if (!enabled() || world == null || Reflect.isRemote(world)) return;
        Map<Long, Integer> floors = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                floors.put(pack(x, z), floorY);
            }
        }
        settle(world, floors);
    }

    public static void settleTemplate(World world, BlockPos origin, BlockPos size, PlacementSettings settings) {
        if (origin == null || size == null) return;
        int[] box = StructureVillageOverlap.aabbAfterRotation(origin, size, settings);
        settleAabb(world, box[0], box[1], box[2], box[3], origin.getY());
    }

    public static void settle(World world, Map<Long, Integer> floorByColumn) {
        settle(world, floorByColumn, false);
    }

    /**
     * @param fillSwampLiquid if true, replace swamp-like water with dirt/grass up to the plate.
     *        Ocean and river biomes are still never written.
     */
    public static void settle(World world, Map<Long, Integer> floorByColumn, boolean fillSwampLiquid) {
        settle(world, floorByColumn, fillSwampLiquid,
                Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.structureRimBank));
    }

    public static void settle(World world, Map<Long, Integer> floorByColumn, boolean fillSwampLiquid, int bank) {
        if (!enabled() || world == null || Reflect.isRemote(world) || floorByColumn == null || floorByColumn.isEmpty()) {
            return;
        }
        int fillDepth = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.structureFillDepth);
        bank = Math.max(0, bank);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int plateY = Integer.MAX_VALUE;
        for (Map.Entry<Long, Integer> e : floorByColumn.entrySet()) {
            int x = unpackX(e.getKey());
            int z = unpackZ(e.getKey());
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
            plateY = Math.min(plateY, e.getValue());
        }
        if (plateY == Integer.MAX_VALUE) return;

        for (Map.Entry<Long, Integer> e : floorByColumn.entrySet()) {
            fillColumn(world, unpackX(e.getKey()), unpackZ(e.getKey()), e.getValue(), fillDepth, true, fillSwampLiquid);
        }

        if (bank <= 0) return;
        int[] box = new int[] {minX, maxX, minZ, maxZ};
        for (int x = minX - bank; x <= maxX + bank; x++) {
            for (int z = minZ - bank; z <= maxZ + bank; z++) {
                if (floorByColumn.containsKey(pack(x, z))) continue;
                double dist = distanceToBoxXZ(x, z, box[0], box[1], box[2], box[3]);
                if (dist <= 0.0 || dist >= bank) continue;
                float blend = blendForDistance(dist, bank);
                if (blend <= 0.0F) continue;
                int ground = surfaceY(world, x, z);
                if (ground <= 0) continue;
                int target = Math.round(ground * (1.0F - blend) + plateY * blend);
                if (target <= ground) continue;
                fillColumn(world, x, z, target + 1, fillDepth, false, fillSwampLiquid);
            }
        }
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    public static int unpackZ(long packed) {
        return (int) packed;
    }

    private static void fillColumn(World world, int x, int z, int floorY, int fillDepth, boolean underStructure,
                                  boolean fillSwampLiquid) {
        if (fillDepth <= 0 || floorY <= 1) return;
        BlockPos surface = new BlockPos(x, Math.max(1, floorY - 1), z);
        Biome biome = world.getBiome(surface);
        if (VillageLandHelper.isNeverRaiseBiome(biome)) return;
        boolean swampFill = fillSwampLiquid && VillageLandHelper.isSwampLikeForRaise(biome);

        IBlockState filler = biome.fillerBlock != null ? biome.fillerBlock : Blocks.DIRT.getDefaultState();
        IBlockState top = biome.topBlock != null ? biome.topBlock : Blocks.GRASS.getDefaultState();
        int minY = Math.max(1, floorY - 1 - fillDepth);
        int topFillY = -1;
        for (int y = floorY - 1; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (isLiquid(state) && !swampFill) {
                break;
            }
            if (!isLiquid(state) && !isFillable(world, pos, state)) {
                break;
            }
            IBlockState place = underStructure || y < floorY - 1 ? filler : top;
            if (!underStructure && y == floorY - 1) place = top;
            world.setBlockState(pos, place, 2);
            topFillY = Math.max(topFillY, y);
        }
        if (!underStructure && topFillY > 0) {
            BlockPos topPos = new BlockPos(x, topFillY, z);
            if (!isLiquid(world.getBlockState(topPos))) {
                world.setBlockState(topPos, top, 2);
            }
        }
        clearPlantsAbove(world, x, z, floorY);
    }

    private static int surfaceY(World world, int x, int z) {
        int y = world.getHeight(x, z);
        while (y > 1) {
            IBlockState state = world.getBlockState(new BlockPos(x, y, z));
            if (!isFillable(world, new BlockPos(x, y, z), state) && !isLiquid(state)) {
                return y;
            }
            y--;
        }
        return y;
    }

    private static boolean isLiquid(IBlockState state) {
        return state != null && state.getMaterial().isLiquid();
    }

    private static void clearPlantsAbove(World world, int x, int z, int floorY) {
        for (int y = floorY; y <= floorY + 3 && y < 256; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (state == null || world.isAirBlock(pos) || state.getBlock() == Blocks.AIR) continue;
            if (isLiquid(state) || !isFillable(world, pos, state)) break;
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    private static boolean isFillable(World world, BlockPos pos, IBlockState state) {
        if (state == null) return true;
        Block block = state.getBlock();
        if (block == Blocks.AIR || world.isAirBlock(pos)) return true;
        if (block == Blocks.SNOW_LAYER || block == Blocks.TALLGRASS || block == Blocks.YELLOW_FLOWER
                || block == Blocks.RED_FLOWER || block == Blocks.DOUBLE_PLANT || block == Blocks.WATERLILY) {
            return true;
        }
        Material mat = state.getMaterial();
        if (mat == Material.LEAVES || mat == Material.WOOD || mat == Material.ROCK) {
            return false;
        }
        if (mat == Material.PLANTS || mat == Material.VINE || mat == Material.CACTUS) {
            return true;
        }
        if (block instanceof BlockBush || block instanceof BlockReed || block instanceof BlockVine) {
            return true;
        }
        try {
            return block.isReplaceable(world, pos);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static float blendForDistance(double dist, int falloff) {
        if (dist <= 0.0) return 1.0F;
        if (falloff <= 0) return 0.0F;
        if (dist >= falloff) return 0.0F;
        float factor = (float) (dist / falloff);
        return 1.0F - (factor * factor * (3.0F - 2.0F * factor));
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
