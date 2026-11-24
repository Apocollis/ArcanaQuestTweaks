package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;

/**
 * Asks the vanilla navigator whether the target can be walked to (plan 3.14), at most once per mage per interval and at most
 * a few probes per server tick across all mages, and keeps the answer in between. The decision itself is
 * {@link ReachabilityRule}.
 */
final class ReachabilityProbe {

    /** Global cap so a crowd of mages cannot make the pathfinder a hot spot. */
    private static final int PER_TICK_CAP = 4;
    private static long capTick = -1L;
    private static int capUsed;

    private boolean blocked;
    private int lastProbeTick = -1000;

    /** The cached verdict, refreshed when the interval has passed and the global cap allows. */
    boolean blocked(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai) {
        int now = mage.ticksExisted;
        if (now - lastProbeTick >= ai.riftPathProbeIntervalTicks && takeProbe(mage.world.getTotalWorldTime())) {
            lastProbeTick = now;
            Path path = mage.getNavigator().getPathToEntityLiving(target);
            double straight = mage.getDistance(target);
            if (path == null) {
                blocked = ReachabilityRule.blocked(false, 0, 0.0D, straight, ai.riftDetourFactor);
            } else {
                PathPoint end = path.getFinalPathPoint();
                double gap = end == null ? 0.0D : Math.sqrt(sq(end.x + 0.5D - target.posX) + sq(end.y - target.posY) + sq(end.z + 0.5D - target.posZ));
                blocked = ReachabilityRule.blocked(true, path.getCurrentPathLength(), gap, straight, ai.riftDetourFactor);
            }
        }
        return blocked;
    }

    void reset() {
        blocked = false;
        lastProbeTick = -1000;
    }

    private static boolean takeProbe(long worldTime) {
        if (worldTime != capTick) {
            capTick = worldTime;
            capUsed = 0;
        }
        if (capUsed >= PER_TICK_CAP) {
            return false;
        }
        capUsed++;
        return true;
    }

    private static double sq(double v) {
        return v * v;
    }
}
