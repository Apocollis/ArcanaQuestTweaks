package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummonUpkeepTest {

    private static FocusSelector.Entry summon(String id, int count) {
        FocusSelector.Entry e = new FocusSelector.Entry(id, "summon_upkeep", 0, 48, false, 1.0D, null, new ArrayList<String>());
        e.summonCount = count;
        return e;
    }

    private static FocusSelector.Entry other(String id, String role) {
        return new FocusSelector.Entry(id, role, 0, 24, true, 1.0D, null, new ArrayList<String>());
    }

    @Test
    void autoDesiredIsHalfACastRoundedUpWithAMinimumOfOne() {
        assertEquals(1, SummonUpkeep.desired(0, 1));
        assertEquals(3, SummonUpkeep.desired(0, 5));
        assertEquals(1, SummonUpkeep.desired(0, 0));
        assertEquals(4, SummonUpkeep.desired(4, 1), "an explicit value wins");
    }

    @Test
    void curseBatMageRecastsAtZeroAndSwarmMageAtTwoOrFewer() {
        assertTrue(SummonUpkeep.needed(0, SummonUpkeep.desired(0, 1)));
        assertFalse(SummonUpkeep.needed(1, SummonUpkeep.desired(0, 1)));
        assertTrue(SummonUpkeep.needed(2, SummonUpkeep.desired(0, 5)));
        assertFalse(SummonUpkeep.needed(3, SummonUpkeep.desired(0, 5)));
    }

    @Test
    void upkeepPicksTheReadySummonOnlyWhenBatsAreShort() {
        List<FocusSelector.Entry> entries = Arrays.asList(other("flame", "ranged_primary"), summon("swarm", 5));
        long[] ready = {0, 0};
        assertEquals(1, FocusSelector.pickUpkeep(entries, ready, 10, 0, 3));
        assertEquals(-1, FocusSelector.pickUpkeep(entries, ready, 10, 3, 3));
    }

    @Test
    void upkeepWaitsOutTheFociOwnCooldown() {
        List<FocusSelector.Entry> entries = Arrays.asList(summon("curse_bat", 1));
        long[] ready = {50};
        assertEquals(-1, FocusSelector.pickUpkeep(entries, ready, 49, 0, 1));
        assertEquals(0, FocusSelector.pickUpkeep(entries, ready, 50, 0, 1));
    }

    @Test
    void upkeepPrefersTheFocusThatSpawnsMostBats() {
        List<FocusSelector.Entry> entries = Arrays.asList(summon("curse_bat", 1), summon("swarm", 5));
        assertEquals(1, FocusSelector.pickUpkeep(entries, new long[] {0, 0}, 0, 0, 3));
        assertEquals(0, FocusSelector.pickUpkeep(entries, new long[] {0, 99}, 0, 0, 3));
    }

    @Test
    void castSizeIsTheLargestSummonInTheLoadout() {
        assertEquals(0, FocusSelector.summonCastSize(Arrays.asList(other("flame", "ranged_primary"))));
        assertEquals(5, FocusSelector.summonCastSize(Arrays.asList(summon("a", 1), summon("b", 5))));
    }

    @Test
    void theSummonRoleIsNeverPickedByTheNormalCastRule() {
        List<FocusSelector.Entry> entries = Arrays.asList(summon("swarm", 5));
        FocusSelector.Context c = new FocusSelector.Context();
        c.distance = 10;
        c.hasLos = true;
        assertEquals(-1, FocusSelector.pick(entries, new long[] {0}, c));
    }
}
