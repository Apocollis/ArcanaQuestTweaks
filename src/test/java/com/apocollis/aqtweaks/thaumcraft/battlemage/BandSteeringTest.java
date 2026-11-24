package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BandSteeringTest {

    @Test
    void holdsTheBand() {
        assertEquals(BandSteering.Move.RETREAT, BandSteering.decide(5.0D, 8.0D, 14.0D, true));
        assertEquals(BandSteering.Move.APPROACH, BandSteering.decide(20.0D, 8.0D, 14.0D, true));
        assertEquals(BandSteering.Move.APPROACH, BandSteering.decide(10.0D, 8.0D, 14.0D, false));
        assertEquals(BandSteering.Move.STRAFE, BandSteering.decide(10.0D, 8.0D, 14.0D, true));
        assertEquals(BandSteering.Move.STRAFE, BandSteering.decide(8.0D, 8.0D, 14.0D, true));
    }
}
