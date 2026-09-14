package com.apocollis.aqtweaks.bettermineshafts;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;

/**
 * Pins {@code /locate Mineshaft} to the shaft origin (or start-chunk tunnel Y).
 * Vanilla mineshaft locate is a can-spawn spiral at Y=64 and never reads Start
 * {@code getPos}.
 */
public final class BetterMineshaftLocate {
    public static final int UNEXPLORED_Y = 24;

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

    public static BlockPos nearest(Object mapGen, BlockPos from, BlockPos vanillaFallback) {
        BlockPos best = retargetFallback(vanillaFallback);
        double bestD = best == null || from == null ? Double.MAX_VALUE : best.distanceSq(from);
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
