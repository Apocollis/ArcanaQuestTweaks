package com.apocollis.aqtweaks.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StarterKitDiffTest {

    private static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("0:0", "wooden_sword");
        m.put("0:1", "dagger_wood");
        m.put("0:8", "quest_book");
        m.put("0:9", "backpack");
        m.put("0:35", "season_clock");
        m.put("0:17", "teleport_potion");
        return m;
    }

    @Test
    void utilitySlotsAreTheSharedOnes() {
        assertTrue(StarterKitDiff.isUtilitySlot("0:8"));
        assertTrue(StarterKitDiff.isUtilitySlot("0:11"));
        assertTrue(StarterKitDiff.isUtilitySlot("0:15"));
        assertTrue(StarterKitDiff.isUtilitySlot("0:27"));
        assertTrue(StarterKitDiff.isUtilitySlot("0:35"));
        assertFalse(StarterKitDiff.isUtilitySlot("0:0"));
        assertFalse(StarterKitDiff.isUtilitySlot("0:7"));
        assertFalse(StarterKitDiff.isUtilitySlot("0:12"));
        assertFalse(StarterKitDiff.isUtilitySlot("1:9"));
        assertFalse(StarterKitDiff.isUtilitySlot("nonsense"));
    }

    @Test
    void aKitThatChangesOnlyTheHotbarHasNoDrift() {
        Map<String, String> kit = new LinkedHashMap<>(defaults());
        kit.put("0:0", "stone_sword");
        kit.remove("0:1");
        kit.put("0:2", "shield");
        assertTrue(StarterKitDiff.drift(defaults(), kit).isEmpty());
    }

    @Test
    void aMissingOrChangedUtilitySlotIsDrift() {
        Map<String, String> kit = new LinkedHashMap<>(defaults());
        kit.remove("0:9");
        kit.put("0:35", "something_else");
        List<String> drift = StarterKitDiff.drift(defaults(), kit);
        assertEquals(2, drift.size());
        assertTrue(drift.contains("0:9"));
        assertTrue(drift.contains("0:35"));
    }

    @Test
    void gearIsTheKitMinusTheDefaultsArmorFirst() {
        Map<String, String> kit = new LinkedHashMap<>(defaults());
        kit.put("0:0", "stone_sword");
        kit.put("0:3", "shield");
        kit.put("1:3", "helmet");
        kit.put("1:2", "chest");
        kit.put("2:0", "offhand_thing");
        List<String> gear = StarterKitDiff.gearSlots(defaults(), kit);
        assertEquals(Arrays.asList("1:2", "1:3", "2:0", "0:0", "0:3"), gear);
    }
}
