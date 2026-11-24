package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Stage 2 additions to the selector: cast budget, cloud refire, opening bonus. */
class FocusSelectorStage2Test {

    private static FocusSelector.Entry entry(String id, String role, double min, double max, int complexity, String opener) {
        FocusSelector.Entry e = new FocusSelector.Entry(id, role, min, max, true, 1.0D, opener, new ArrayList<String>());
        e.complexity = complexity;
        return e;
    }

    private static FocusSelector.Context ctx(double dist) {
        FocusSelector.Context c = new FocusSelector.Context();
        c.distance = dist;
        c.hasLos = true;
        c.tick = 100;
        c.random = new Random(1);
        return c;
    }

    @Test
    void holdingBlocksEveryBudgetedCast() {
        List<FocusSelector.Entry> entries = Arrays.asList(entry("flame", "ranged_primary", 3, 24, 25, null));
        FocusSelector.Context c = ctx(10);
        c.holding = true;
        assertEquals(-1, FocusSelector.pick(entries, new long[] {0}, c));
        c.holding = false;
        assertEquals(0, FocusSelector.pick(entries, new long[] {0}, c));
    }

    @Test
    void aFocusThatCostsMoreThanTheEnergyIsSkippedButACheaperOneIsNot() {
        List<FocusSelector.Entry> entries = Arrays.asList(
                entry("heavy", "bolt_heavy", 2, 16, 50, null),
                entry("flux", "bolt", 2, 16, 20, null));
        FocusSelector.Context c = ctx(10);
        c.maxEnergy = 100;
        c.energy = 30;
        assertEquals(1, FocusSelector.pick(entries, new long[] {0, 0}, c));
    }

    @Test
    void aCloudWaitsWhileTheMageStillOwnsOne() {
        FocusSelector.Entry cloud = entry("tempest", "aoe", 14, 30, 50, null);
        cloud.refireOnExpire = true;
        List<FocusSelector.Entry> entries = Arrays.asList(cloud);
        FocusSelector.Context c = ctx(20);
        c.ownedClouds = 1;
        assertEquals(-1, FocusSelector.pick(entries, new long[] {0}, c));
        c.ownedClouds = 0;
        assertEquals(0, FocusSelector.pick(entries, new long[] {0}, c));
        cloud.refireOnExpire = false;
        c.ownedClouds = 2;
        assertEquals(0, FocusSelector.pick(entries, new long[] {0}, c));
    }

    @Test
    void theOpeningCastPrefersAnOpener() {
        List<FocusSelector.Entry> entries = Arrays.asList(
                entry("flux", "bolt", 2, 16, 20, null),
                entry("flame", "ranged_primary", 3, 24, 25, "fire"));
        FocusSelector.Context c = ctx(10);
        c.opening = true;
        for (int seed = 0; seed < 50; seed++) {
            c.random = new Random(seed);
            assertEquals(1, FocusSelector.pick(entries, new long[] {0, 0}, c), "seed " + seed);
        }
    }
}
