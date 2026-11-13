package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SurgeMathTest {
    @Test
    void scourTiersAndScaling() {
        assertEquals(4.0D, SurgeMath.scour(false, 4.0D, 1.5D, 1.0D, 1.0D), 1e-9);
        assertEquals(1.5D, SurgeMath.scour(true, 4.0D, 1.5D, 1.0D, 1.0D), 1e-9);
        assertEquals(2.0D, SurgeMath.scour(false, 4.0D, 1.5D, 0.5D, 1.0D), 1e-9);
        assertEquals(4.8D, SurgeMath.scour(false, 4.0D, 1.5D, 1.0D, 1.2D), 1e-9);
    }

    @Test
    void chargeIsAPercentOfCapacityScaledByExposure() {
        assertEquals(100.0D, SurgeMath.chargeAmount(1000.0D, 10.0D, 1.0D, 1.0D), 1e-9);
        assertEquals(50.0D, SurgeMath.chargeAmount(1000.0D, 10.0D, 0.5D, 1.0D), 1e-9);
    }

    @Test
    void warpIsCappedPerSurge() {
        assertEquals(1, SurgeMath.warpAllowed(0, 1, 1));
        assertEquals(0, SurgeMath.warpAllowed(1, 1, 1));
        assertEquals(2, SurgeMath.warpAllowed(10, 5, 12));
        assertEquals(0, SurgeMath.warpAllowed(12, 1, 12));
    }

    @Test
    void backlashScalesWithExposureAndTakesTheProperTier() {
        assertEquals(3.0D, SurgeMath.backlash(3.0D, 1.0D, false, 4.0D, 1.5D), 1e-9);
        assertEquals(1.5D, SurgeMath.backlash(3.0D, 0.5D, false, 4.0D, 1.5D), 1e-9);
        assertEquals(3.0D * 1.5D / 4.0D, SurgeMath.backlash(3.0D, 1.0D, true, 4.0D, 1.5D), 1e-9);
        assertEquals(0.0D, SurgeMath.backlash(3.0D, 0.0D, false, 4.0D, 1.5D), 1e-9);
    }
}
