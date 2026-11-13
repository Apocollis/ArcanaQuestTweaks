package com.apocollis.aqtweaks.hazard;

/** Timing ranges of a channel in ticks, read from the module config each time an event is drawn. */
public final class HazardTiming {
    public final long clearMin;
    public final long clearMax;
    public final long durationMin;
    public final long durationMax;
    public final int warning;
    public final int rampUp;
    public final int rampDown;

    public HazardTiming(long clearMin, long clearMax, long durationMin, long durationMax, int warning, int rampUp,
                        int rampDown) {
        this.clearMin = Math.max(1L, clearMin);
        this.clearMax = Math.max(this.clearMin, clearMax);
        this.durationMin = Math.max(1L, durationMin);
        this.durationMax = Math.max(this.durationMin, durationMax);
        this.warning = Math.max(0, warning);
        this.rampUp = Math.max(0, rampUp);
        this.rampDown = Math.max(0, rampDown);
    }
}
