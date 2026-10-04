package com.apocollis.aqtweaks.bettermineshafts;

import net.minecraft.block.BlockLadder;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/**
 * Fallback surface opening for a Better Mineshafts entrance that could not build its own (typical on
 * RTG terrain: BM refuses when the surface is below Y60 or has no drop-off). Without it the mineshaft
 * is sealed, with no way in from the surface.
 *
 * <p>The opening is a 1x1 ladder shaft from the tunnel level up to the ground, backed by planks where
 * the wall is open, with a raised plank collar around the hole at the surface so it can be found.
 * Nothing is built under water. Each call writes only inside {@code clip}, so a shaft that straddles a
 * chunk edge is finished by the neighboring chunk's paste.
 */
public final class MineshaftSurfaceShaft {

    /** The shaft must rise at least this far to be worth building; shorter ones keep the stub. */
    private static final int MIN_RISE = 3;

    private MineshaftSurfaceShaft() {}

    /**
     * Ground level above the shaft column: the first air block over the top solid/liquid block, or
     * -1 when the column is not loaded, is under water, or is too close to the tunnel to need a shaft.
     */
    public static int surfaceY(World world, BlockPos center) {
        if (world == null || center == null) return -1;
        BlockPos column = new BlockPos(center.getX(), 0, center.getZ());
        if (!world.isBlockLoaded(column)) return -1;
        BlockPos top = world.getTopSolidOrLiquidBlock(column);
        int surface = top.getY();
        if (surface < center.getY() + MIN_RISE) return -1;
        IBlockState ground = world.getBlockState(top.down());
        if (ground.getMaterial().isLiquid()) return -1;
        return surface;
    }

    /** Box that holds the shaft and its collar: tunnel level to one block above the ground. */
    public static StructureBoundingBox boxFor(BlockPos center, int surfaceY) {
        return new StructureBoundingBox(center.getX() - 1, center.getY(), center.getZ() - 1,
                center.getX() + 1, surfaceY + 1, center.getZ() + 1);
    }

    /** Carve the shaft, ladder and collar inside {@code clip}. */
    public static void carve(World world, BlockPos center, int surfaceY, StructureBoundingBox clip) {
        int x = center.getX();
        int z = center.getZ();
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState planks = Blocks.PLANKS.getDefaultState();
        IBlockState ladder = Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, EnumFacing.NORTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = center.getY(); y < surfaceY; y++) {
            if (inside(clip, x, y, z + 1)) {
                // Wall behind the ladder (south side); ladders need a solid block to hang on.
                IBlockState back = world.getBlockState(pos.setPos(x, y, z + 1));
                if (!back.getMaterial().isSolid() || back.getMaterial() == Material.LEAVES) {
                    world.setBlockState(pos, planks, 2);
                }
            }
            if (inside(clip, x, y, z)) {
                world.setBlockState(pos.setPos(x, y, z), air, 2);
                world.setBlockState(pos, ladder, 2);
            }
        }
        // Open air at ground level so the hole is walkable, then the collar around it.
        if (inside(clip, x, surfaceY, z)) {
            world.setBlockState(pos.setPos(x, surfaceY, z), air, 2);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (inside(clip, x + dx, surfaceY, z + dz)) {
                    world.setBlockState(pos.setPos(x + dx, surfaceY, z + dz), planks, 2);
                }
            }
        }
    }

    private static final java.util.Set<Long> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("AQTweaks-Mineshaft");

    /**
     * Build the surface shaft for a Better Mineshafts Start whose entrance was refused (the entrance
     * piece reports {@code aqtweaks$isFallback}). The shaft is aimed at the mineshaft itself: if the
     * hub column lies inside a sibling tunnel/room box it drops straight down to the hub; otherwise it is
     * moved to the nearest sibling box and drills into that box (floor + 1). Runs once per chunk paste
     * and writes only inside {@code clip}; the target is a pure function of the Start, so neighboring
     * chunks agree on the column.
     */
    public static void carveForStart(World world, net.minecraft.world.gen.structure.StructureStart start,
                                     StructureBoundingBox clip) {
        VerticalEntranceAccess entrance = null;
        java.util.List<StructureBoundingBox> siblings = new java.util.ArrayList<>();
        for (net.minecraft.world.gen.structure.StructureComponent piece : start.getComponents()) {
            if (piece instanceof VerticalEntranceAccess access) {
                if (access.aqtweaks$isFallback()) entrance = access;
                continue;
            }
            StructureBoundingBox box = piece.getBoundingBox();
            if (box != null) siblings.add(box);
        }
        if (entrance == null) return;
        BlockPos hub = entrance.aqtweaks$getCenterPos();
        if (hub == null) return;

        int tx = hub.getX();
        int tz = hub.getZ();
        int bottom = hub.getY();
        StructureBoundingBox target = null;
        double best = Double.MAX_VALUE;
        boolean hubInside = false;
        for (StructureBoundingBox box : siblings) {
            if (box.isVecInside(hub)) {
                hubInside = true;
                break;
            }
            double dx = Math.max(Math.max(box.minX - hub.getX(), 0), hub.getX() - box.maxX);
            double dz = Math.max(Math.max(box.minZ - hub.getZ(), 0), hub.getZ() - box.maxZ);
            double d = dx * dx + dz * dz;
            if (d < best) {
                best = d;
                target = box;
            }
        }
        if (!hubInside && target != null) {
            tx = clampInner(hub.getX(), target.minX, target.maxX);
            tz = clampInner(hub.getZ(), target.minZ, target.maxZ);
            bottom = target.minY + 1;
        }

        BlockPos base = new BlockPos(tx, bottom, tz);
        int surface = surfaceY(world, base);
        if (LOGGED.size() < 256 && LOGGED.add(((long) hub.getX() << 32) ^ (hub.getZ() & 0xFFFFFFFFL))) {
            LOGGER.info("[AQ-MINESHAFT] surface shaft hub={},{},{} target={},{},{} surfaceY={} siblings={} hubInside={}",
                    hub.getX(), hub.getY(), hub.getZ(), tx, bottom, tz, surface, siblings.size(), hubInside);
        }
        if (surface < 0) return;
        carve(world, base, surface, clip);
    }

    /** {@code v} moved inside {@code [min + 1, max - 1]} (the middle when the box is too narrow). */
    private static int clampInner(int v, int min, int max) {
        if (max - min < 2) return (min + max) / 2;
        return Math.max(min + 1, Math.min(max - 1, v));
    }

    private static boolean inside(StructureBoundingBox clip, int x, int y, int z) {
        return clip == null || clip.isVecInside(new BlockPos(x, y, z));
    }
}
