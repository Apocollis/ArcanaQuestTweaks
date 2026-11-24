package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.List;

/**
 * Picks which player the mage fights (plan 3.11 item 3): the one that hit it last, the lowest health, one that is casting
 * or healing, with a stickiness bonus so it does not flip every re-pick. Pure: candidates are plain numbers.
 */
public final class TargetScorer {

    /** A player the mage could fight. */
    public static final class Candidate {
        public final int id;
        public double distance;
        public double healthFraction = 1.0D;
        public boolean hitMeLast;
        public boolean busy;
        public boolean hasLos;
        public boolean current;

        public Candidate(int id) {
            this.id = id;
        }
    }

    public static final double STICKY_BONUS = 4.0D;
    public static final double HIT_ME_BONUS = 5.0D;
    public static final double BUSY_BONUS = 2.0D;
    public static final double LOW_HEALTH_WEIGHT = 3.0D;
    public static final double LOS_BONUS = 1.5D;
    /** One point of score per this many blocks of distance. */
    public static final double DISTANCE_SCALE = 10.0D;

    private TargetScorer() {}

    public static double score(Candidate c) {
        double s = 0.0D;
        s -= c.distance / DISTANCE_SCALE * 4.0D;
        s += (1.0D - Math.max(0.0D, Math.min(1.0D, c.healthFraction))) * LOW_HEALTH_WEIGHT;
        if (c.hitMeLast) {
            s += HIT_ME_BONUS;
        }
        if (c.busy) {
            s += BUSY_BONUS;
        }
        if (c.hasLos) {
            s += LOS_BONUS;
        }
        if (c.current) {
            s += STICKY_BONUS;
        }
        return s;
    }

    /** The id of the best candidate, or -1 when the list is empty. Ties keep the earlier entry. */
    public static int pick(List<Candidate> candidates) {
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Candidate c : candidates) {
            double s = score(c);
            if (s > bestScore) {
                bestScore = s;
                best = c.id;
            }
        }
        return best;
    }
}
