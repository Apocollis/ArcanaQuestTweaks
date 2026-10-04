package com.apocollis.aqtweaks.bettermineshafts;

import com.apocollis.aqtweaks.rtg.StructureAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Pins {@code /locate Mineshaft} to the nearest of: a registered Start, or the next can-spawn chunk
 * that is not already generated empty. Both are ranked by distance, like vanilla ranks predicted
 * positions. Candidates within {@link #SKIP_RADIUS} blocks (XZ) of the player are skipped so a
 * repeat {@code /locate} from inside a mineshaft moves on to the next one instead of returning the
 * same Start forever.
 */
public final class BetterMineshaftLocate {
    public static final int UNEXPLORED_Y = 24;
    private static final int SPIRAL_LIMIT = 1000;
    /** Horizontal blocks. A pin this close is the mineshaft the player is already at. */
    static final int SKIP_RADIUS = 32;

    private BetterMineshaftLocate() {}

    public static BlockPos pin(Object start) {
        if (start == null) {
            return null;
        }
        for (Object piece : StructureAccess.getStructureStartComponents(start)) {
            if (piece instanceof VerticalEntranceAccess access) {
                BlockPos center = access.aqtweaks$getCenterPos();
                if (center != null) {
                    return center;
                }
            }
        }
        int cx = StructureAccess.getStructureStartChunkX(start);
        int cz = StructureAccess.getStructureStartChunkZ(start);
        if (cx == Integer.MIN_VALUE || cz == Integer.MIN_VALUE) {
            return null;
        }
        int y = StructureAccess.getStructureStartMinY(start);
        if (y == Integer.MIN_VALUE) {
            y = UNEXPLORED_Y - 4;
        }
        return new BlockPos((cx << 4) + 8, y + 4, (cz << 4) + 8);
    }

    public static BlockPos locate(Object mapGen, World world, BlockPos from, BlockPos vanillaFallback,
                                 boolean findUnexplored) {
        if (world != null) {
            StructureAccess.setMapGenWorld(mapGen, world);
            StructureAccess.initializeStructureData(mapGen, world);
        }
        // "Find unexplored" must not hand back a Start that already exists.
        BlockPos registered = findUnexplored ? null : nearestRegistered(mapGen, from);
        BlockPos vanilla = retargetFallback(vanillaFallback);
        BlockPos predicted = !tooClose(vanilla, from) && isUsablePrediction(mapGen, world, vanilla, findUnexplored)
                ? vanilla
                : firstPrediction(mapGen, world, from, findUnexplored);
        return closer(from, registered, predicted);
    }

    /** The candidate nearer to {@code from}; the other when one is null. */
    static BlockPos closer(BlockPos from, BlockPos a, BlockPos b) {
        if (a == null) return b;
        if (b == null) return a;
        if (from == null) return a;
        return a.distanceSq(from) <= b.distanceSq(from) ? a : b;
    }

    /** True when {@code pin} is within {@link #SKIP_RADIUS} blocks (XZ) of {@code from}. */
    static boolean tooClose(BlockPos pin, BlockPos from) {
        if (pin == null || from == null) return false;
        long dx = (long) pin.getX() - from.getX();
        long dz = (long) pin.getZ() - from.getZ();
        return dx * dx + dz * dz <= (long) SKIP_RADIUS * SKIP_RADIUS;
    }

    static BlockPos nearestRegistered(Object mapGen, BlockPos from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (Object start : StructureAccess.getMapGenStructureStarts(mapGen)) {
            BlockPos candidate = pin(start);
            if (candidate == null || from == null || tooClose(candidate, from)) {
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
        if (generated && !StructureAccess.hasStructureStart(mapGen, cx, cz)) {
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
                    if (tooClose(pin, from)) {
                        continue;
                    }
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
