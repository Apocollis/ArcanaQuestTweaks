package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.thaumcraft.battlemage.FocusSelector.Context;
import com.apocollis.aqtweaks.thaumcraft.battlemage.FocusSelector.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FocusSelectorTest {

    private static Entry e(String id, String role, double min, double max, boolean los, String opener, String... followUp) {
        return new Entry(id, role, min, max, los, 1.0D, opener, Arrays.asList(followUp));
    }

    private static List<Entry> kit() {
        return Arrays.asList(
                e("frost", "ranged_primary", 3, 24, true, "slow"),
                e("flux", "bolt", 2, 16, true, null),
                e("prismatic", "bolt_heavy", 2, 16, true, null, "stunned", "slowed", "wet"),
                e("cloud", "aoe", 14, 30, false, null),
                e("heal", "heal_self", 0, 0, false, null),
                e("blink", "blink", 0, 20, false, null));
    }

    private static Context ctx(double dist, boolean los, long tick) {
        Context c = new Context();
        c.distance = dist;
        c.hasLos = los;
        c.tick = tick;
        c.random = new Random(7);
        return c;
    }

    @Test
    void nothingReadyPicksNothing() {
        long[] ready = {100, 100, 100, 100, 100, 100};
        assertEquals(-1, FocusSelector.pick(kit(), ready, ctx(10, true, 50)));
    }

    @Test
    void cooldownsForceSwitchingAcrossFoci() {
        long[] ready = new long[6];
        ready[1] = 60;
        ready[2] = 60;
        assertEquals(0, FocusSelector.pick(kit(), ready, ctx(10, true, 10)));
        ready[0] = 60;
        assertEquals(-1, FocusSelector.pick(kit(), ready, ctx(10, true, 10)));
        int pick = FocusSelector.pick(kit(), ready, ctx(10, true, 60));
        assertTrue(pick == 0 || pick == 1, "picked " + pick);
    }

    @Test
    void nonCombatRolesAreNeverCastInStageOne() {
        long[] ready = new long[6];
        for (int i = 0; i < 200; i++) {
            Context c = ctx(10, true, 10);
            c.random = new Random(i);
            int pick = FocusSelector.pick(kit(), ready, c);
            assertTrue(pick == 0 || pick == 1 || pick == 2, "picked " + pick);
        }
    }

    @Test
    void aoeOnlyBeyondTheLongRangeThreshold() {
        long[] ready = new long[6];
        Context near = ctx(10, true, 10);
        near.aoeMinRange = 14.0D;
        assertTrue(FocusSelector.pick(kit(), ready, near) != 3);
        Context far = ctx(20, false, 10);
        far.aoeMinRange = 14.0D;
        assertEquals(3, FocusSelector.pick(kit(), ready, far));
    }

    @Test
    void lineOfSightIsRequiredWhereAsked() {
        long[] ready = new long[6];
        assertEquals(-1, FocusSelector.pick(kit(), ready, ctx(10, false, 10)));
    }

    @Test
    void rangeBandsFilter() {
        long[] ready = new long[6];
        for (int i = 0; i < 50; i++) {
            Context close = ctx(2.5D, true, 10);
            close.random = new Random(i);
            assertEquals(1, FocusSelector.pick(kit(), ready, close));
        }
    }

    @Test
    void heavyFocusWaitsForAFollowUpState() {
        long[] ready = new long[6];
        int heavyWithoutState = 0;
        int heavyWithState = 0;
        for (int i = 0; i < 300; i++) {
            Context plain = ctx(10, true, 10);
            plain.random = new Random(i);
            if (FocusSelector.pick(kit(), ready, plain) == 2) {
                heavyWithoutState++;
            }
            Context stunned = ctx(10, true, 10);
            stunned.random = new Random(i);
            stunned.targetStates = new HashSet<>(Collections.singletonList("stunned"));
            if (FocusSelector.pick(kit(), ready, stunned) == 2) {
                heavyWithState++;
            }
        }
        assertEquals(0, heavyWithoutState);
        assertTrue(heavyWithState > 250, "heavy picked " + heavyWithState);
    }

    @Test
    void varietyAvoidsRepeatingTheLastFocus() {
        long[] ready = new long[6];
        List<Entry> two = new ArrayList<>(Arrays.asList(
                e("a", "ranged_primary", 0, 30, false, null), e("b", "ranged_primary", 0, 30, false, null)));
        int b = 0;
        for (int i = 0; i < 200; i++) {
            Context c = ctx(10, true, 10);
            c.random = new Random(i);
            c.lastFocusId = "a";
            if (FocusSelector.pick(two, new long[2], c) == 1) {
                b++;
            }
        }
        assertEquals(200, b);
    }
}
