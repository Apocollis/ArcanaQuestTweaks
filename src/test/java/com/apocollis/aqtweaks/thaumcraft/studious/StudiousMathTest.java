package com.apocollis.aqtweaks.thaumcraft.studious;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class StudiousMathTest {

    private static TreeMap<String, Integer> totals(Object... kv) {
        TreeMap<String, Integer> map = new TreeMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return map;
    }

    @Test
    void leadingCategoryIsTheHighestTotal() {
        assertEquals("ALCHEMY", StudiousMath.leadingCategory(totals("BASICS", 20, "ALCHEMY", 35, "ARTIFICE", 10)));
        assertNull(StudiousMath.leadingCategory(totals()));
        assertNull(StudiousMath.leadingCategory(null));
        assertNull(StudiousMath.leadingCategory(totals("BASICS", 0)));
    }

    @Test
    void tiesGoToTheFirstKey() {
        assertEquals("ALCHEMY", StudiousMath.leadingCategory(totals("ALCHEMY", 30, "BASICS", 30)));
    }

    @Test
    void eurekaNeedsTheLeaderToReachTheThreshold() {
        assertFalse(StudiousMath.eurekaEligible(totals("BASICS", 29), 30));
        assertTrue(StudiousMath.eurekaEligible(totals("BASICS", 30), 30));
        assertTrue(StudiousMath.eurekaEligible(totals("BASICS", 12, "ALCHEMY", 41), 30));
        assertFalse(StudiousMath.eurekaEligible(totals(), 30));
        assertFalse(StudiousMath.eurekaEligible(null, 30));
    }

    @Test
    void crossReferenceTargetsAreTheStrongestFirstAndCapped() {
        TreeMap<String, Integer> t = totals("A", 5, "B", 40, "C", 20, "D", 20, "E", 1, "F", 0);
        assertEquals(Arrays.asList("B", "C", "D", "A"), StudiousMath.crossReferenceTargets(t, 4));
        assertEquals(Arrays.asList("B", "C", "D", "A", "E"), StudiousMath.crossReferenceTargets(t, 10));
        assertEquals(Collections.emptyList(), StudiousMath.crossReferenceTargets(t, 0));
        assertEquals(Collections.emptyList(), StudiousMath.crossReferenceTargets(null, 4));
    }

    @Test
    void crossReferenceNeedsEnoughCategories() {
        assertFalse(StudiousMath.crossReferenceEligible(totals("A", 20), 2));
        assertTrue(StudiousMath.crossReferenceEligible(totals("A", 20, "B", 5), 2));
        assertFalse(StudiousMath.crossReferenceEligible(totals("A", 20, "B", 0), 2));
    }

    @Test
    void steadyFocusShortensCooldownsWithAFloor() {
        assertEquals(75, StudiousMath.scaleCooldown(100, 0.25D));
        assertEquals(100, StudiousMath.scaleCooldown(100, 0.0D));
        assertEquals(0, StudiousMath.scaleCooldown(0, 0.25D));
        assertEquals(1, StudiousMath.scaleCooldown(1, 0.25D));
        assertEquals(10, StudiousMath.scaleCooldown(100, 5.0D));
        assertEquals(100, StudiousMath.scaleCooldown(100, -0.5D));
    }
}
