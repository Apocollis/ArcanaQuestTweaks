package com.apocollis.aqtweaks.hazard;

/**
 * One scheduled hazard event, announced by the server and animated by every client from the world clock.
 * All ticks are absolute {@code World.getTotalWorldTime()} values. No Minecraft types.
 *
 * <p>Layout: {@code warningStart <= start < end}. The warning runs from {@code warningStart} to {@code start};
 * the event is active from {@code start} to {@code end}, ramping up over {@code rampUp} ticks after
 * {@code start} and down over {@code rampDown} ticks before {@code end}.
 */
public final class HazardEvent {

    public enum Phase { CLEAR, WARNING, ACTIVE }

    public final int channel;
    public final long id;
    public final long warningStart;
    public final long start;
    public final long end;
    public final int rampUp;
    public final int rampDown;
    /** For headings and visuals every client must reproduce. */
    public final long seed;
    public final float intensity;
    public final int flags;

    public HazardEvent(int channel, long id, long warningStart, long start, long end, int rampUp, int rampDown,
                       long seed, float intensity, int flags) {
        this.channel = channel;
        this.id = id;
        this.start = start;
        this.warningStart = Math.min(warningStart, start);
        this.end = Math.max(end, start);
        this.rampUp = Math.max(0, rampUp);
        this.rampDown = Math.max(0, rampDown);
        this.seed = seed;
        this.intensity = intensity;
        this.flags = flags;
    }

    public Phase phaseAt(long tick) {
        if (tick >= end || tick < warningStart) return Phase.CLEAR;
        return tick >= start ? Phase.ACTIVE : Phase.WARNING;
    }

    /** 0..1 over the warning window, 0 once the event has started. */
    public float warningAt(long tick) {
        if (tick < warningStart || tick >= start) return 0.0F;
        long length = start - warningStart;
        if (length <= 0L) return 0.0F;
        return smooth((float) (tick - warningStart) / (float) length);
    }

    /** 0..1, eased, with the separate ramps at both ends of the active window. */
    public float strengthAt(long tick) {
        if (tick < start || tick >= end) return 0.0F;
        float up = rampUp <= 0 ? 1.0F : Math.min(1.0F, (float) (tick - start) / (float) rampUp);
        float down = rampDown <= 0 ? 1.0F : Math.min(1.0F, (float) (end - tick) / (float) rampDown);
        return smooth(Math.min(up, down));
    }

    public long ticksToStart(long tick) {
        return Math.max(0L, start - tick);
    }

    public long ticksToEnd(long tick) {
        return Math.max(0L, end - tick);
    }

    public HazardEvent withEnd(long newId, long newEnd, int newRampDown) {
        return new HazardEvent(channel, newId, warningStart, start, newEnd, rampUp, newRampDown, seed, intensity,
                flags);
    }

    static float smooth(float x) {
        float c = Math.max(0.0F, Math.min(1.0F, x));
        return c * c * (3.0F - 2.0F * c);
    }

    @Override
    public String toString() {
        return "HazardEvent{ch=" + channel + ", id=" + id + ", warn=" + warningStart + ", start=" + start
                + ", end=" + end + "}";
    }
}
