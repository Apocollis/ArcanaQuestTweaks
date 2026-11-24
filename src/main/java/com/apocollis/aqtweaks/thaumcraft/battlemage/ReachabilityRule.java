package com.apocollis.aqtweaks.thaumcraft.battlemage;

/** The pure rule behind {@link ReachabilityProbe} (plan 3.14): is the target reachable on foot at a sane cost? */
public final class ReachabilityRule {

    /** A path shorter than this many nodes is never "too long". */
    public static final int MIN_NODES = 16;

    private ReachabilityRule() {}

    /**
     * @param hasPath the navigator found a path at all
     * @param nodes number of nodes of the path
     * @param endGap distance from the path's last node to the target (a path that stops short does not reach it)
     * @param straight straight-line distance mage to target
     * @param detourFactor reachable when the path is at most this times the straight distance (and at least {@link #MIN_NODES})
     * @return true when the target is blocked: no path, a path that stops short, or a detour that is too long
     */
    public static boolean blocked(boolean hasPath, int nodes, double endGap, double straight, double detourFactor) {
        if (!hasPath) {
            return true;
        }
        if (endGap > 2.5D) {
            return true;
        }
        double limit = Math.max(MIN_NODES, detourFactor * straight);
        return nodes > limit;
    }
}
