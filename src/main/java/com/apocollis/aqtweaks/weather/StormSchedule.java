package com.apocollis.aqtweaks.weather;

/**
 * Deterministic storm timetable shared by server and client, with no Minecraft types.
 *
 * <p>Time is cut into segments. Segment {@code i} is a clear interval followed by a storm; both lengths
 * are hashes of {@code (key, i)} mapped into the configured ranges, so two sides that agree on the key and
 * the tick agree on the weather without sending any storm packet. The state is advanced from a cached
 * segment cursor (it recomputes from tick 0 only if time moves backwards).
 */
public final class StormSchedule {

    /** Result for one tick. {@code segStart <= stormStart <= stormEnd} are absolute effective ticks. */
    public static final class State {
        public final long tick;
        public final long segStart;
        public final long stormStart;
        public final long stormEnd;
        /** True while inside the storm part of the segment, including the ramp up and down. */
        public final boolean storming;
        /** 0..1, eased. Ramps over {@code rampTicks} at both ends of the storm. */
        public final float strength;
        /** 0..1 over the {@code warningTicks} before a storm, 0 during a storm. */
        public final float warning;
        /** Ticks until the next storm starts (the storm after this one while storming). */
        public final long ticksToNextStorm;

        State(long tick, long segStart, long stormStart, long stormEnd, boolean storming, float strength,
              float warning, long ticksToNextStorm) {
            this.tick = tick;
            this.segStart = segStart;
            this.stormStart = stormStart;
            this.stormEnd = stormEnd;
            this.storming = storming;
            this.strength = strength;
            this.warning = warning;
            this.ticksToNextStorm = ticksToNextStorm;
        }
    }

    /** The state used when storms are off or not yet known. */
    public static final State CLEAR = new State(0L, 0L, Long.MAX_VALUE, Long.MAX_VALUE, false, 0.0F, 0.0F,
            Long.MAX_VALUE);

    private final long key;
    private final long clearMin;
    private final long clearMax;
    private final long stormMin;
    private final long stormMax;
    private final int rampTicks;
    private final int warningTicks;

    private long cursorIndex;
    private long cursorStart;
    private State last;

    public StormSchedule(long key, long clearMinTicks, long clearMaxTicks, long stormMinTicks, long stormMaxTicks,
                         int rampTicks, int warningTicks) {
        this.key = key;
        this.clearMin = Math.max(1L, clearMinTicks);
        this.clearMax = Math.max(this.clearMin, clearMaxTicks);
        this.stormMin = Math.max(1L, stormMinTicks);
        this.stormMax = Math.max(this.stormMin, stormMaxTicks);
        this.rampTicks = Math.max(1, rampTicks);
        this.warningTicks = Math.max(0, warningTicks);
    }

    public boolean sameParams(long key, long clearMinTicks, long clearMaxTicks, long stormMinTicks,
                              long stormMaxTicks, int rampTicks, int warningTicks) {
        return this.key == key
                && this.clearMin == Math.max(1L, clearMinTicks)
                && this.clearMax == Math.max(Math.max(1L, clearMinTicks), clearMaxTicks)
                && this.stormMin == Math.max(1L, stormMinTicks)
                && this.stormMax == Math.max(Math.max(1L, stormMinTicks), stormMaxTicks)
                && this.rampTicks == Math.max(1, rampTicks)
                && this.warningTicks == Math.max(0, warningTicks);
    }

    /** Clear-interval length of segment {@code index}, in ticks. */
    public long clearLength(long index) {
        return lerp(clearMin, clearMax, fraction(key, index * 2L));
    }

    /** Storm length of segment {@code index}, in ticks. */
    public long stormLength(long index) {
        return lerp(stormMin, stormMax, fraction(key, index * 2L + 1L));
    }

    public synchronized State at(long tick) {
        if (tick < 0L) {
            tick = 0L;
        }
        if (last != null && last.tick == tick) {
            return last;
        }
        if (tick < cursorStart) {
            cursorIndex = 0L;
            cursorStart = 0L;
        }
        while (true) {
            long length = clearLength(cursorIndex) + stormLength(cursorIndex);
            if (tick < cursorStart + length) {
                break;
            }
            cursorStart += length;
            cursorIndex++;
        }
        long segStart = cursorStart;
        long stormStart = segStart + clearLength(cursorIndex);
        long stormEnd = stormStart + stormLength(cursorIndex);
        boolean storming = tick >= stormStart;
        float strength = 0.0F;
        float warning = 0.0F;
        long toNext;
        if (storming) {
            long into = tick - stormStart;
            long length = stormEnd - stormStart;
            float up = Math.min(1.0F, (float) into / (float) rampTicks);
            float down = Math.min(1.0F, (float) (length - into) / (float) rampTicks);
            strength = smooth(Math.min(up, down));
            toNext = stormEnd + clearLength(cursorIndex + 1L) - tick;
        } else {
            long remaining = stormStart - tick;
            if (warningTicks > 0 && remaining < warningTicks) {
                warning = 1.0F - (float) remaining / (float) warningTicks;
            }
            toNext = remaining;
        }
        last = new State(tick, segStart, stormStart, stormEnd, storming, strength, warning, toNext);
        return last;
    }

    static float smooth(float x) {
        float c = Math.max(0.0F, Math.min(1.0F, x));
        return c * c * (3.0F - 2.0F * c);
    }

    private static long lerp(long min, long max, double fraction) {
        return min + (long) (fraction * (double) (max - min));
    }

    /** Uniform value in [0, 1) from a splitmix64 hash of the key and an index. */
    static double fraction(long key, long index) {
        long z = key + index * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (double) (z >>> 11) * (1.0D / (double) (1L << 53));
    }
}
