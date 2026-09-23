package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

/**
 * Village paths kept over a river by {@link VillageLandHelper#isBridgeablePath}: after the Start pastes
 * into a clip, swap vanilla's water-level planks for a stone brick deck at plate Y, with cobblestone
 * wall rails on a 1-block brick lip and brick piers down to the bed.
 */
public final class VillageBridges {

    private static final int CLEAR_ABOVE = 3;
    private static final int PIER_EVERY = 4;
    private static final int SCAN_ABOVE = 6;
    private static final int SCAN_MIN_Y = 20;

    private VillageBridges() {}

    public static void afterVillagePaste(World world, StructureStart start, StructureBoundingBox clip, float plate) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageRiverBridges) return;
        if (world == null || clip == null || world.isRemote || start == null) return;
        List<StructureComponent> components = start.getComponents();
        if (components == null || components.isEmpty()) return;

        int decks = 0;
        VillageLandHelper.pushColumnLandscapeCache();
        try {
            for (StructureComponent component : components) {
                if (component == null || !VillageLandHelper.isVillageRoad(component)) continue;
                StructureBoundingBox bb = component.getBoundingBox();
                if (bb == null) continue;
                if (bb.maxX + 1 < clip.minX || bb.minX - 1 > clip.maxX
                        || bb.maxZ + 1 < clip.minZ || bb.minZ - 1 > clip.maxZ) {
                    continue;
                }
                decks += bridgeRoad(world, bb, clip, plate);
            }
        } finally {
            VillageLandHelper.popColumnLandscapeCache();
        }
        if (decks > 0 && VillageDebug.enabled()) {
            VillageDebug.log("bridge clip=[%d,%d]x[%d,%d] plate=%.1f deckColumns=%d",
                    clip.minX, clip.maxX, clip.minZ, clip.maxZ, plate, decks);
        }
    }

    private static int bridgeRoad(World world, StructureBoundingBox bb, StructureBoundingBox clip, float plate) {
        boolean alongX = bb.maxX - bb.minX >= bb.maxZ - bb.minZ;
        int axisMin = alongX ? bb.minX : bb.minZ;
        int axisMax = alongX ? bb.maxX : bb.maxZ;
        int crossMin = alongX ? bb.minZ : bb.minX;
        int crossMax = alongX ? bb.maxZ : bb.maxX;
        boolean[] pierAt = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int decks = 0;
        for (int a = axisMin; a <= axisMax; a++) {
            for (int c = crossMin; c <= crossMax; c++) {
                int x = alongX ? a : c;
                int z = alongX ? c : a;
                if (!inClipXZ(clip, x, z)) continue;
                int surfaceY = waterSurfaceAfterRevert(world, pos, x, z, plate);
                if (surfaceY < 0) continue;
                int deckY = deckY(plate, surfaceY);
                if (deckY > 250) continue;
                pos.setPos(x, deckY, z);
                world.setBlockState(pos, Blocks.STONEBRICK.getDefaultState(), 2);
                clearAbove(world, pos, x, deckY, z);
                decks++;

                if (c != crossMin && c != crossMax) continue;
                int side = c == crossMin ? c - 1 : c + 1;
                int sx = alongX ? a : side;
                int sz = alongX ? side : a;
                if (!inClipXZ(clip, sx, sz)) continue;
                if (pierAt == null) {
                    pierAt = pierPlan(world, alongX, axisMin, axisMax, crossMin, crossMax);
                }
                railColumn(world, pos, sx, sz, deckY, pierAt[a - axisMin]);
            }
        }
        return decks;
    }

    /**
     * Top liquid Y of the column, or -1 if it is land. Vanilla {@code Path} laid planks on the water
     * surface; those are turned back into water first.
     */
    private static int waterSurfaceAfterRevert(World world, BlockPos.MutableBlockPos pos, int x, int z, float plate) {
        int top = Float.isNaN(plate) ? world.getSeaLevel() : Math.max(world.getSeaLevel(), Math.round(plate));
        for (int y = Math.min(255, top + SCAN_ABOVE); y >= SCAN_MIN_Y; y--) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (airLike(state)) continue;
            Material mat = state.getMaterial();
            if (mat == Material.WATER) return y;
            if (mat == Material.WOOD) {
                pos.setPos(x, y - 1, z);
                if (world.getBlockState(pos).getMaterial() == Material.WATER) {
                    pos.setPos(x, y, z);
                    world.setBlockState(pos, Blocks.WATER.getDefaultState(), 2);
                    return y;
                }
            }
            return -1;
        }
        return -1;
    }

    private static int deckY(float plate, int surfaceY) {
        if (Float.isNaN(plate)) return surfaceY + 2;
        return Math.max(Math.round(plate), surfaceY + 1);
    }

    private static void clearAbove(World world, BlockPos.MutableBlockPos pos, int x, int deckY, int z) {
        for (int y = deckY + 1; y <= Math.min(255, deckY + CLEAR_ABOVE); y++) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial() == Material.AIR || !airLike(state)) continue;
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    private static void railColumn(World world, BlockPos.MutableBlockPos pos, int x, int z, int deckY, boolean pier) {
        pos.setPos(x, deckY, z);
        if (!airLike(world.getBlockState(pos)) && !world.getBlockState(pos).getMaterial().isLiquid()) return;
        world.setBlockState(pos, Blocks.STONEBRICK.getDefaultState(), 2);
        pos.setPos(x, deckY + 1, z);
        if (airLike(world.getBlockState(pos))) {
            world.setBlockState(pos, Blocks.COBBLESTONE_WALL.getDefaultState(), 2);
        }
        if (!pier) return;
        for (int y = deckY - 1; y >= SCAN_MIN_Y; y--) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            if (!airLike(state) && !state.getMaterial().isLiquid()) break;
            world.setBlockState(pos, Blocks.STONEBRICK.getDefaultState(), 2);
        }
    }

    /**
     * Pier slices along the road, from landscape water runs so every clip of one road agrees.
     * Runs of {@link #PIER_EVERY} or less get none.
     */
    private static boolean[] pierPlan(World world, boolean alongX, int axisMin, int axisMax, int crossMin, int crossMax) {
        int len = axisMax - axisMin + 1;
        boolean[] wet = new boolean[len];
        BiomeProvider provider = world.getBiomeProvider();
        for (int i = 0; i < len; i++) {
            int a = axisMin + i;
            for (int c = crossMin; c <= crossMax && !wet[i]; c++) {
                int x = alongX ? a : c;
                int z = alongX ? c : a;
                wet[i] = VillageLandHelper.isVillageWaterColumn(world, provider, null, x, z);
            }
        }
        boolean[] pier = new boolean[len];
        int i = 0;
        while (i < len) {
            if (!wet[i]) {
                i++;
                continue;
            }
            int runStart = i;
            while (i < len && wet[i]) i++;
            int runLen = i - runStart;
            if (runLen <= PIER_EVERY) continue;
            for (int off = PIER_EVERY - 1; off < runLen - 1; off += PIER_EVERY) {
                pier[runStart + off] = true;
            }
        }
        return pier;
    }

    private static boolean inClipXZ(StructureBoundingBox clip, int x, int z) {
        return x >= clip.minX && x <= clip.maxX && z >= clip.minZ && z <= clip.maxZ;
    }

    private static boolean airLike(IBlockState state) {
        if (state == null) return true;
        Material mat = state.getMaterial();
        return mat == Material.AIR || mat == Material.PLANTS || mat == Material.VINE
                || mat == Material.LEAVES || mat == Material.SNOW;
    }
}
