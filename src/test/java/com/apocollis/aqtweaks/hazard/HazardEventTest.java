package com.apocollis.aqtweaks.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HazardEventTest {
    private final HazardEvent e = new HazardEvent(0, 1, 1000, 1600, 3000, 200, 100, 7L, 1.0F, 0);

    @Test
    void phases() {
        assertEquals(HazardEvent.Phase.CLEAR, e.phaseAt(999));
        assertEquals(HazardEvent.Phase.WARNING, e.phaseAt(1000));
        assertEquals(HazardEvent.Phase.WARNING, e.phaseAt(1599));
        assertEquals(HazardEvent.Phase.ACTIVE, e.phaseAt(1600));
        assertEquals(HazardEvent.Phase.ACTIVE, e.phaseAt(2999));
        assertEquals(HazardEvent.Phase.CLEAR, e.phaseAt(3000));
    }

    @Test
    void warningRisesThenStops() {
        assertEquals(0.0F, e.warningAt(999));
        assertEquals(0.5F, e.warningAt(1300), 0.001F);
        assertTrue(e.warningAt(1599) > 0.99F);
        assertEquals(0.0F, e.warningAt(1600));
    }

    @Test
    void strengthUsesSeparateRamps() {
        assertEquals(0.0F, e.strengthAt(1599));
        assertEquals(0.5F, e.strengthAt(1700), 0.001F);
        assertEquals(1.0F, e.strengthAt(2000));
        assertEquals(0.5F, e.strengthAt(2950), 0.001F);
        assertEquals(0.0F, e.strengthAt(3000));
    }

    @Test
    void zeroRampsAreInstant() {
        HazardEvent s = new HazardEvent(0, 1, 0, 10, 20, 0, 0, 0L, 1.0F, 0);
        assertEquals(1.0F, s.strengthAt(10));
        assertEquals(1.0F, s.strengthAt(19));
    }
}
