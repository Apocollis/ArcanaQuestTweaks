package com.apocollis.aqtweaks.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.aether.AetherSquallGear.GearClass;

import org.junit.jupiter.api.Test;

class StormWarningMathTest {
    @Test
    void motesGrowWithTheWarningAndWithGear() {
        assertEquals(1, StormWarningMath.moteCount(0.0F, GearClass.NORMAL));
        assertEquals(6, StormWarningMath.moteCount(1.0F, GearClass.NORMAL));
        assertEquals(12, StormWarningMath.moteCount(1.0F, GearClass.CONDUCTIVE));
        assertEquals(18, StormWarningMath.moteCount(1.0F, GearClass.CHANNELING));
        assertTrue(StormWarningMath.moteCount(0.5F, GearClass.NORMAL) > StormWarningMath.moteCount(0.0F, GearClass.NORMAL));
    }

    @Test
    void thunderTightensAndGetsLouder() {
        assertEquals(80, StormWarningMath.thunderGapTicks(0.0F));
        assertEquals(30, StormWarningMath.thunderGapTicks(1.0F));
        assertEquals(0.15F, StormWarningMath.thunderVolume(0.0F), 1e-6F);
        assertEquals(0.6F, StormWarningMath.thunderVolume(1.0F), 1e-6F);
        assertEquals(StormWarningMath.thunderGapTicks(0.0F), StormWarningMath.thunderGapTicks(-3.0F));
    }
}
