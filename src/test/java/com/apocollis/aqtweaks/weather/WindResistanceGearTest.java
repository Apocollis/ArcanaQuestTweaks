package com.apocollis.aqtweaks.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WindResistanceGearTest {

    @Test
    void weightReductionCurve() {
        assertEquals(0.0D, WindResistanceGear.weightReduction(0.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.10D, WindResistanceGear.weightReduction(4.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.20D, WindResistanceGear.weightReduction(8.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.275D, WindResistanceGear.weightReduction(11.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.30D, WindResistanceGear.weightReduction(12.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.40D, WindResistanceGear.weightReduction(16.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.50D, WindResistanceGear.weightReduction(20.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        assertEquals(0.50D, WindResistanceGear.weightReduction(30.0D, 0.0D, 20.0D, 0.50D), 1e-9);
        // Step when maxWeightRef <= threshold
        assertEquals(0.50D, WindResistanceGear.weightReduction(5.0D, 4.0D, 4.0D, 0.50D), 1e-9);
        assertEquals(0.0D, WindResistanceGear.weightReduction(4.0D, 4.0D, 4.0D, 0.50D), 1e-9);
    }

    @Test
    void windResistantPiecesScaleLinearly() {
        for (int pieces = 0; pieces <= 4; pieces++) {
            double f = WindResistanceGear.pushFactor(
                    0.0D, pieces, false, true, false, 0.70D,
                    true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D);
            assertEquals(1.0D - pieces * 0.10D, f, 1e-9);
        }
    }

    @Test
    void anchoringBootsApplyOnlyOnGround() {
        double grounded = WindResistanceGear.pushFactor(
                0.0D, 0, true, true, false, 0.70D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D);
        double airborne = WindResistanceGear.pushFactor(
                0.0D, 0, true, false, false, 0.70D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D);
        assertEquals(0.75D, grounded, 1e-9);
        assertEquals(1.00D, airborne, 1e-9);
    }

    @Test
    void additiveStackingWithSneakAndZeroFloor() {
        // Naked sneaking (70% sneak reduction -> 0.30 factor)
        assertEquals(0.30D, WindResistanceGear.pushFactor(
                0.0D, 0, false, true, true, 0.70D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);

        // Iron (weight 8 -> 20%) + sneaking (70%) = 90% reduction -> 0.10 factor
        assertEquals(0.10D, WindResistanceGear.pushFactor(
                8.0D, 0, false, true, true, 0.70D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);

        // Iron (20%) + Anchoring grounded (25%) + sneaking (70%) = 115% -> clamped to 0.0
        assertEquals(0.0D, WindResistanceGear.pushFactor(
                8.0D, 0, true, true, true, 0.70D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);

        // Airborne removes anchoring bonus: Iron (20%) + sneaking (70%) -> 0.10
        assertEquals(0.10D, WindResistanceGear.pushFactor(
                8.0D, 0, true, false, true, 0.70D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);

        // Full Wind Resistant (40%) + Obsidian (30%) + Anchoring grounded (25%) = 95% without sneaking -> 0.05
        assertEquals(0.05D, WindResistanceGear.pushFactor(
                12.0D, 4, true, true, false, 0.75D,
                true, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);
    }

    @Test
    void togglesDisableGearOrWeightWhilePreservingSneak() {
        // Master toggle off: only sneak applies
        assertEquals(1.0D, WindResistanceGear.pushFactor(
                20.0D, 4, true, true, false, 0.75D,
                false, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);
        assertEquals(0.25D, WindResistanceGear.pushFactor(
                20.0D, 4, true, true, true, 0.75D,
                false, true, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);

        // Weight toggle off: Wind Resistant + Anchoring still apply
        assertEquals(0.35D, WindResistanceGear.pushFactor(
                20.0D, 4, true, true, false, 0.75D,
                true, false, 0.0D, 20.0D, 0.50D, 0.40D, 0.25D), 1e-9);
    }
}
