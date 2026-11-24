package com.apocollis.aqtweaks.twilightforest;

/**
 * Deterministic fractional level gain for the Vis Battery (pure). 0.25 per cycle is one level per four cycles; the
 * fraction carries over, so the total over a surge is exact instead of random.
 */
public final class LevelAccumulator {
    private LevelAccumulator() {}

    public static final class Step {
        public final int levels;
        public final double carry;

        Step(int levels, double carry) {
            this.levels = levels;
            this.carry = carry;
        }
    }

    /**
     * @param carry the fraction carried from earlier cycles
     * @param add   what this cycle adds (levels per cycle x exposure x biome)
     * @param room  levels the battery can still take (10 minus its charge)
     */
    public static Step step(double carry, double add, int room) {
        if (room <= 0) {
            return new Step(0, 0.0D);
        }
        double total = Math.max(0.0D, carry) + Math.max(0.0D, add);
        int levels = Math.min((int) Math.floor(total), room);
        return new Step(levels, total - levels);
    }
}
