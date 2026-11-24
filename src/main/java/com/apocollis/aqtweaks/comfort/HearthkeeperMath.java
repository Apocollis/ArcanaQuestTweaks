package com.apocollis.aqtweaks.comfort;

/**
 * Pure numbers for the Homesteader's Hearthkeeper perk (starter-class plan 13.6): the Homesteader's own diminishing
 * company bonus, the home box score bonus and the Carry the Warmth duration.
 */
public final class HearthkeeperMath {

    private HearthkeeperMath() {}

    /**
     * The Homesteader's comfort bonus for {@code others} other players nearby. The first players follow
     * {@code curve} in order, every further player adds {@code tail}, and the total never passes {@code cap}
     * (a cap of zero or less means no cap).
     */
    public static double companyBonus(int others, double[] curve, double tail, double cap) {
        if (others <= 0) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (int i = 0; i < others; i++) {
            double step = curve != null && i < curve.length ? curve[i] : tail;
            sum += Math.max(0.0D, step);
        }
        return cap > 0.0D ? Math.min(cap, sum) : sum;
    }

    /** Home box bonus: a fraction of the cozy score, never negative. */
    public static double boxBonus(double cozyScore, double fraction) {
        return Math.max(0.0D, cozyScore) * Math.max(0.0D, fraction);
    }

    /** Carry the Warmth: the Rested duration given at refresh, lengthened by {@code extra} (0.5 is half again). */
    public static int lingerDuration(int baseTicks, double extra) {
        if (baseTicks <= 0 || extra <= 0.0D) {
            return baseTicks;
        }
        return (int) Math.round(baseTicks * (1.0D + extra));
    }
}
