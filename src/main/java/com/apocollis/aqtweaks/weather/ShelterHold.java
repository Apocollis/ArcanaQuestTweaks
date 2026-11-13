package com.apocollis.aqtweaks.weather;

/**
 * Holds a boolean shelter state until the raw value has differed for a delay, so ducking under a branch or past
 * a doorway does not flip the storm effects. No Minecraft types.
 */
public final class ShelterHold {
    private boolean held;
    private boolean initialised;
    private long pendingSince = -1L;

    /** Returns the held state after seeing {@code raw} at {@code tick}. A delay of 0 or less passes it through. */
    public boolean update(boolean raw, long tick, long delayTicks) {
        if (!initialised || delayTicks <= 0L) {
            held = raw;
            initialised = true;
            pendingSince = -1L;
            return held;
        }
        if (raw == held) {
            pendingSince = -1L;
            return held;
        }
        if (pendingSince < 0L) {
            pendingSince = tick;
        }
        if (tick - pendingSince >= delayTicks) {
            held = raw;
            pendingSince = -1L;
        }
        return held;
    }

    public void reset() {
        initialised = false;
        pendingSince = -1L;
    }
}
