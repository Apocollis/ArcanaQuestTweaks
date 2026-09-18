package com.apocollis.aqtweaks.bettermineshafts;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Pins {@code /locate Mineshaft} to a registered Start, or to the next can-spawn
 * chunk that is not already generated empty.
 */
public final class BetterMineshaftLocate {
    public static final int UNEXPLORED_Y = 24;
    private static final int SPIRAL_LIMIT = 1000;

    private BetterMineshaftLocate() {}

    public static BlockPos pin(Object start) {
        if (start == null) {
            return null;
        }
        for (Object piece : Reflect.getStructureStartComponents(start)) {
            if (piece instanceof VerticalEntranceAccess access) {
                BlockPos center = access.aqtweaks$getCenterPos();
                if (center != null) {
                    return center;
                }
            }
        }
        int cx = Reflect.getStructureStartChunkX(start);
        int cz = Reflect.getStructureStartChunkZ(start);
        if (cx == Integer.MIN_VALUE || cz == Integer.MIN_VALUE) {
            return null;
        }
        int y = Reflect.getStructureStartMinY(start);
        if (y == Integer.MIN_VALUE) {
            y = UNEXPLORED_Y - 4;
        }
        return new BlockPos((cx << 4) + 8, y + 4, (cz << 4) + 8);
    }

    public static BlockPos locate(Object mapGen, World world, BlockPos from, BlockPos vanillaFallback,
                                 boolean findUnexplored) {
        if (world != null) {
            Reflect.setMapGenWorld(mapGen, world);
            Reflect.initializeStructureData(mapGen, world);
        }
        BlockPos registered = nearestRegistered(mapGen, from);
        if (registered != null) {
            return registered;
        }
        BlockPos vanilla = retargetFallback(vanillaFallback);
        if (isUsablePrediction(mapGen, world, vanilla, findUnexplored)) {
            return vanilla;
        }
        return firstPrediction(mapGen, world, from, findUnexplored);
    }

    static BlockPos nearestRegistered(Object mapGen, BlockPos from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (Object start : Reflect.getMapGenStructureStarts(mapGen)) {
            BlockPos candidate = pin(start);
            if (candidate == null || from == null) {
                continue;
            }
            double d = candidate.distanceSq(from);
            if (d < bestD) {
                bestD = d;
                best = candidate;
            }
        }
        return best;
    }

    static boolean isUsablePrediction(Object mapGen, World world, BlockPos pos, boolean findUnexplored) {
        if (pos == null || world == null) {
            return false;
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        boolean generated = world.isChunkGeneratedAt(cx, cz);
        if (findUnexplored && generated) {
            return false;
        }
        if (generated && !Reflect.hasStructureStart(mapGen, cx, cz)) {
            return false;
        }
        return true;
    }

    static BlockPos firstPrediction(Object mapGen, World world, BlockPos from, boolean findUnexplored) {
        if (world == null || from == null) {
            return null;
        }
        int originX = from.getX() >> 4;
        int originZ = from.getZ() >> 4;
        for (int ring = 0; ring <= SPIRAL_LIMIT; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                boolean edgeX = dx == -ring || dx == ring;
                for (int dz = -ring; dz <= ring; dz++) {
                    boolean edgeZ = dz == -ring || dz == ring;
                    if (!edgeX && !edgeZ) {
                        continue;
                    }
                    int cx = originX + dx;
                    int cz = originZ + dz;
                    Boolean spawn = BetterMineshaftCanSpawn.test(mapGen, cx, cz);
                    if (!Boolean.TRUE.equals(spawn)) {
                        continue;
                    }
                    BlockPos pin = new BlockPos((cx << 4) + 8, UNEXPLORED_Y, (cz << 4) + 8);
                    if (isUsablePrediction(mapGen, world, pin, findUnexplored)) {
                        return pin;
                    }
                }
            }
        }
        return null;
    }

    public static BlockPos retargetFallback(BlockPos vanilla) {
        if (vanilla == null) {
            return null;
        }
        if (vanilla.getY() == 64) {
            return new BlockPos(vanilla.getX(), UNEXPLORED_Y, vanilla.getZ());
        }
        return vanilla;
    }
}
