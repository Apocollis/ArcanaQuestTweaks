package com.apocollis.aqtweaks.hazard;

/** What a handler needs about one channel at the world's current tick. Same shape on both sides. */
public final class HazardState {

    public static final HazardState NONE = new HazardState(null, 0L, HazardEvent.Phase.CLEAR, 0.0F, 0.0F);

    /** Null when nothing is scheduled or known. */
    public final HazardEvent event;
    public final long tick;
    public final HazardEvent.Phase phase;
    /** 0..1 during the warning, otherwise 0. */
    public final float warning;
    /** 0..1 while active, with the ramps. */
    public final float strength;
    /** True from the event start to its end (ramps included). */
    public final boolean storming;
    public final long ticksToEnd;

    public HazardState(HazardEvent event, long tick, HazardEvent.Phase phase, float warning, float strength) {
        this.event = event;
        this.tick = tick;
        this.phase = phase;
        this.warning = warning;
        this.strength = strength;
        this.storming = phase == HazardEvent.Phase.ACTIVE;
        this.ticksToEnd = event == null ? 0L : event.ticksToEnd(tick);
    }

    public static HazardState of(HazardEvent event, long tick) {
        if (event == null) return NONE;
        return new HazardState(event, tick, event.phaseAt(tick), event.warningAt(tick), event.strengthAt(tick));
    }
}
