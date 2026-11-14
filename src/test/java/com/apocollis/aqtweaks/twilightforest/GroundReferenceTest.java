package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroundReferenceTest {
    @Test
    void referenceIsTheAverageMinusTheOffset() {
        assertEquals(30.0D, GroundReference.reference(new int[] {32, 32, 32, 32, 32}, 2), 1e-9);
        assertEquals(31.0D, GroundReference.reference(new int[] {30, 34, 32, 30, 34}, 1), 1e-9);
    }

    @Test
    void fullBelowAndAtTheReferenceNoneAtTheFadeHeight() {
        assertEquals(1.0D, GroundReference.exposure(10, 30, 14), 1e-9);
        assertEquals(1.0D, GroundReference.exposure(30, 30, 14), 1e-9);
        assertEquals(0.0D, GroundReference.exposure(44, 30, 14), 1e-9);
        assertEquals(0.0D, GroundReference.exposure(90, 30, 14), 1e-9);
    }

    @Test
    void linearGradientInBetween() {
        assertEquals(0.5D, GroundReference.exposure(37, 30, 14), 1e-9);
    }

    @Test
    void aPlayerOnALeafCountsTheGroundBelowTheCanopy() {
        // Ground at Y 32; leaves from 60 to 70 and a player standing on a leaf at Y 71.
        int surface = GroundReference.surfaceY(80, 0, y -> y <= 32);
        assertEquals(32, surface);
        double reference = GroundReference.reference(new int[] {surface}, 2);
        assertEquals(0.0D, GroundReference.exposure(71, reference, 14), 1e-9);
        assertEquals(1.0D - 7.0D / 14.0D, GroundReference.exposure(37, reference, 14), 1e-9);
    }

    @Test
    void aCaveIsBelowTheReference() {
        double reference = GroundReference.reference(new int[] {40, 40, 40, 40, 40}, 2);
        assertEquals(1.0D, GroundReference.exposure(20, reference, 14), 1e-9);
    }

    @Test
    void defaultFadeHeightReachesAboutYFiftyOverTypicalForestGround() {
        // Reference 30 (ground 32 minus the offset 2) and the default fade height 22.
        assertEquals(1.0D, GroundReference.exposure(30, 30, 22), 1e-9);
        assertEquals(0.545D, GroundReference.exposure(40, 30, 22), 1e-3);
        assertEquals(0.09D, GroundReference.exposure(50, 30, 22), 1e-2);
        assertEquals(0.0D, GroundReference.exposure(52, 30, 22), 1e-9);
        assertEquals(0.0D, GroundReference.exposure(55, 30, 22), 1e-9);
    }
}
