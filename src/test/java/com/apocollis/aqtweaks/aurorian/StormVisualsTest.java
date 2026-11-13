package com.apocollis.aqtweaks.aurorian;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import org.junit.jupiter.api.Test;

class StormVisualsTest {
    private final HazardEvent e = new HazardEvent(0, 1, 1000, 1600, 5000, 600, 600, 1L, 1.0F, 0);

    private float at(long t) {
        return StormVisuals.scale(HazardState.of(e, t), 0.6F);
    }

    @Test
    void zeroOutsideTheEvent() {
        assertEquals(0.0F, at(500));
        assertEquals(0.0F, at(5000));
    }

    @Test
    void warningRisesToTheFraction() {
        assertTrue(at(1300) > 0.0F);
        assertEquals(0.6F, at(1599), 0.02F);
    }

    @Test
    void noDipAtTheStart() {
        assertEquals(at(1599), at(1600), 0.02F);
        assertTrue(at(1700) >= 0.45F);
    }

    @Test
    void reachesFullAndFadesOut() {
        assertEquals(1.0F, at(3000), 0.001F);
        assertTrue(at(4900) < 0.1F);
    }
}
