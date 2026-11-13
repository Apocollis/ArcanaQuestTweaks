package com.apocollis.aqtweaks.aurorian;

import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;

/** Visual intensity of a storm for fog, particles and sound, continuous through warning, start and end. No Minecraft types. */
public final class StormVisuals {
    private StormVisuals() {}

    /**
     * 0..1. During the warning it is {@code fraction * warning}. After the start it is the larger of the storm
     * strength and a lead-in that falls from {@code fraction} to 0 over the ramp-up, so there is no dip between
     * the warning and the rising storm.
     */
    public static float scale(HazardState state, float fraction) {
        if (state == null || state.event == null) return 0.0F;
        if (state.phase == HazardEvent.Phase.WARNING) {
            return fraction * state.warning;
        }
        if (state.phase == HazardEvent.Phase.ACTIVE) {
            float elapsed = state.tick - state.event.start;
            float ramp = Math.max(1, state.event.rampUp);
            float lead = fraction * Math.max(0.0F, 1.0F - elapsed / ramp);
            return Math.max(state.strength, lead);
        }
        return 0.0F;
    }
}
