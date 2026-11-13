package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AshDoseTrackerTest {
    @Test
    void buildsInsideAndDecaysOutside() {
        double d = 0;
        for (int i = 0; i < 5; i++) d = AshDoseTracker.update(d, true, 1.0D, 20.0D);
        assertEquals(5.0D, d, 1e-9);
        d = AshDoseTracker.update(d, false, 1.0D, 20.0D);
        assertEquals(3.0D, d, 1e-9);
    }

    @Test
    void cappedAndNeverNegative() {
        assertEquals(20.0D, AshDoseTracker.update(19.8D, true, 1.0D, 20.0D), 1e-9);
        assertEquals(0.0D, AshDoseTracker.update(0.5D, false, 1.0D, 20.0D), 1e-9);
    }

    @Test
    void thresholds() {
        assertEquals(0, AshDoseTracker.level(4.9D, 5, 12, 20));
        assertEquals(1, AshDoseTracker.level(5.0D, 5, 12, 20));
        assertEquals(2, AshDoseTracker.level(12.0D, 5, 12, 20));
        assertEquals(3, AshDoseTracker.level(20.0D, 5, 12, 20));
    }

    @Test
    void properGearBuildsAQuarterAsFast() {
        double exposed = 0;
        double proper = 0;
        for (int i = 0; i < 20; i++) {
            exposed = AshDoseTracker.update(exposed, true, 1.0D, 100.0D);
            proper = AshDoseTracker.update(proper, true, 0.25D, 100.0D);
        }
        assertEquals(4.0D, exposed / proper, 1e-9);
    }
}
