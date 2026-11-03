package com.apocollis.aqtweaks.rtg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class VillageBridgeRuleTest {

    private static final int PAD = 12;
    private static final int SPAN = 16;

    private static VillageBridgeRule.Land house(String tag, int minX, int maxX, int minZ, int maxZ) {
        return new VillageBridgeRule.Land(tag, new int[] {minX, maxX, minZ, maxZ}, true, false);
    }

    private static VillageBridgeRule.Land well(String tag, int minX, int maxX, int minZ, int maxZ) {
        return new VillageBridgeRule.Land(tag, new int[] {minX, maxX, minZ, maxZ}, true, true);
    }

    private static VillageBridgeRule.Land road(String tag, int minX, int maxX, int minZ, int maxZ) {
        return new VillageBridgeRule.Land(tag, new int[] {minX, maxX, minZ, maxZ}, false, false);
    }

    /** East-west bridge on z=0 from x=start; wet marks the slices that are water. */
    private static VillageBridgeRule.Wet bridge(String tag, int start, String slices) {
        boolean[] wet = new boolean[slices.length()];
        for (int i = 0; i < wet.length; i++) wet[i] = slices.charAt(i) == 'w';
        return new VillageBridgeRule.Wet(tag, true, start, 0, wet, false);
    }

    private static String dry(int n) {
        char[] c = new char[n];
        Arrays.fill(c, 'd');
        return new String(c);
    }

    private static String water(int n) {
        char[] c = new char[n];
        Arrays.fill(c, 'w');
        return new String(c);
    }

    private static List<VillageBridgeRule.Land> lands(VillageBridgeRule.Land... l) {
        return new ArrayList<VillageBridgeRule.Land>(Arrays.asList(l));
    }

    private static List<VillageBridgeRule.Wet> wets(VillageBridgeRule.Wet... w) {
        return new ArrayList<VillageBridgeRule.Wet>(Arrays.asList(w));
    }

    @Test
    void riverBetweenTwoStructurePlatesIsKept() {
        // water x 13..27 starts inside the well pad; far house at x 35..
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("far", 35, 45, 0, 8));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(8) + water(15) + dry(10));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN);
        assertTrue(r.keptRoads.contains("b"), String.valueOf(r.droppedRoads));
        assertTrue(r.orphans.isEmpty());
    }

    @Test
    void waterStartingPastThePadIsDropped() {
        // well box ends x=4, pad+1 reaches x=17; water begins at x=25
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("far", 60, 70, 0, 8));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(20) + water(10) + dry(20));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN);
        assertEquals("no-near-plate", r.droppedRoads.get("b"));
    }

    @Test
    void waterWithNoDryColumnAfterItIsDropped() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(8) + water(10));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN);
        assertEquals("wet-end", r.droppedRoads.get("b"));
    }

    @Test
    void oceanIsDropped() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4));
        VillageBridgeRule.Wet b = new VillageBridgeRule.Wet("b", true, 5, 0, new boolean[30], true);
        assertEquals("ocean", VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN).droppedRoads.get("b"));
    }

    @Test
    void farPlateWithoutStructureIsDropped() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), road("stub", 30, 50, 0, 2));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(8) + water(10) + dry(20));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN);
        assertEquals("no-structure", r.droppedRoads.get("b"));
        assertTrue(r.orphans.contains("stub"));
    }

    @Test
    void bridgeToNothingAtTheFarEndIsDropped() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("far", 200, 210, 0, 8));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(8) + water(10) + dry(20));
        assertEquals("no-far-plate", VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN).droppedRoads.get("b"));
    }

    @Test
    void islandJoinedOnlyByTheBridgeIsKept() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("island", 35, 45, 0, 8));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(8) + water(14) + dry(20));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN);
        assertTrue(r.keptRoads.contains("b"), String.valueOf(r.droppedRoads));
    }

    @Test
    void chainOfTwoBridgesIsKept() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("mid", 35, 45, 0, 8),
                house("end", 75, 85, 0, 8));
        VillageBridgeRule.Wet b1 = bridge("b1", 5, dry(8) + water(14) + dry(10));
        VillageBridgeRule.Wet b2 = bridge("b2", 46, dry(8) + water(14) + dry(10));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b1, b2), PAD, SPAN);
        assertTrue(r.keptRoads.contains("b1") && r.keptRoads.contains("b2"), String.valueOf(r.droppedRoads));
    }

    @Test
    void secondBridgeFromADroppedFirstOneIsUnreachable() {
        // b1 fails (no structure at its far side); b2 hangs off the far side of b1 only
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), road("mid", 35, 45, 0, 2),
                house("end", 75, 85, 0, 8));
        VillageBridgeRule.Wet b1 = bridge("b1", 5, dry(8) + water(14) + dry(10));
        VillageBridgeRule.Wet b2 = bridge("b2", 46, dry(8) + water(14) + dry(10));
        VillageBridgeRule.Result r = VillageBridgeRule.evaluate(l, wets(b1, b2), PAD, SPAN);
        assertTrue(r.droppedRoads.containsKey("b1"));
        assertTrue(r.droppedRoads.containsKey("b2"));
        assertTrue(r.orphans.contains("end") && r.orphans.contains("mid"));
    }

    @Test
    void spanOverTheLimitIsDropped() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("far", 40, 50, 0, 8));
        VillageBridgeRule.Wet b = bridge("b", 5, dry(8) + water(20) + dry(10));
        assertEquals("span", VillageBridgeRule.evaluate(l, wets(b), PAD, SPAN).droppedRoads.get("b"));
    }

    @Test
    void unjoinedPlateIsLeftAlone() {
        List<VillageBridgeRule.Land> l = lands(well("well", 0, 4, 0, 4), house("stray", 300, 310, 0, 8));
        assertFalse(VillageBridgeRule.evaluate(l, wets(), PAD, SPAN).orphans.contains("stray"));
    }
}
