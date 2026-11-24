package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StunGateTest {

    @Test
    void firstStunIsHalvedAndTheImmunityStartsWhenItEnds() {
        StunGate g = new StunGate();
        assertEquals(30, g.adjust(100, 60, 0.5, 80));
        assertEquals(0, g.adjust(101, 60, 0.5, 80), "still stunned, no re-stun");
        assertEquals(0, g.adjust(100 + 30 + 79, 60, 0.5, 80), "inside the 4 s window after it ends");
        assertEquals(30, g.adjust(100 + 30 + 80, 60, 0.5, 80), "window over");
    }

    @Test
    void zeroScaleOrZeroTicksIgnoresTheStun() {
        StunGate g = new StunGate();
        assertEquals(0, g.adjust(0, 60, 0.0, 80));
        assertEquals(0, g.adjust(0, 0, 0.5, 80));
    }

    @Test
    void aShortStunStaysAtLeastOneTick() {
        assertEquals(1, new StunGate().adjust(0, 1, 0.5, 0));
    }
}
