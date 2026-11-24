package com.apocollis.aqtweaks.stamina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningHoldTest {

    private static int pay(MiningHold.Session s, long from, long to, int gap, int interval) {
        int total = 0;
        for (long t = from; t <= to; t++) {
            total += MiningHold.touch(s, t, gap, interval);
        }
        return total;
    }

    @Test
    void theStartOfASessionIsPaidAtOnce() {
        MiningHold.Session s = new MiningHold.Session();
        assertEquals(1, MiningHold.touch(s, 100, 10, 10));
        assertEquals(0, MiningHold.touch(s, 101, 10, 10));
    }

    @Test
    void aHoldPaysEveryIntervalStartingWithTheFirstTick() {
        MiningHold.Session s = new MiningHold.Session();
        // 100 ticks of digging: payments at ticks 0, 10, 20 ... 90.
        assertEquals(10, pay(s, 0, 99, 10, 10));
    }

    @Test
    void miningEfficiencyHalvesTheDrain() {
        MiningHold.Session s = new MiningHold.Session();
        assertEquals(5, pay(s, 0, 99, 10, 20));
    }

    @Test
    void movingToTheNextBlockDoesNotRestartTheTimer() {
        MiningHold.Session s = new MiningHold.Session();
        int total = 0;
        // Ten blocks, each 2 ticks of digging then a 5 tick delay: 70 ticks, activity on the dig ticks only.
        for (int block = 0; block < 10; block++) {
            long start = block * 7L;
            total += MiningHold.touch(s, start, 10, 10);
            total += MiningHold.touch(s, start + 1, 10, 10);
        }
        // A fast tool: one session over 70 ticks, a payment whenever 10 ticks have passed since the last one.
        assertTrue(total >= 4 && total <= 8, "payments " + total);
        assertTrue(total < 10, "cheaper per block than one payment each");
    }

    @Test
    void aQuietGapStartsANewSessionAndPaysAgain() {
        MiningHold.Session s = new MiningHold.Session();
        assertEquals(1, MiningHold.touch(s, 0, 10, 10));
        assertEquals(0, MiningHold.touch(s, 5, 10, 10));
        // 11 ticks without activity: a new session.
        assertEquals(1, MiningHold.touch(s, 16, 10, 10));
    }

    @Test
    void tappingSingleBlocksPaysEveryTap() {
        MiningHold.Session s = new MiningHold.Session();
        int total = 0;
        for (int tap = 0; tap < 5; tap++) {
            total += MiningHold.touch(s, tap * 40L, 10, 10);
        }
        assertEquals(5, total);
    }

    @Test
    void hardnessDecidesBetweenHoldFlatAndUnbreakable() {
        assertTrue(MiningHold.billedByHold(1.5F));
        assertTrue(MiningHold.billedByHold(0.2F));
        assertFalse(MiningHold.billedByHold(0.0F));
        assertFalse(MiningHold.billedByHold(-1.0F));
        assertTrue(MiningHold.flatPlant(0.0F));
        assertFalse(MiningHold.flatPlant(0.2F));
        assertFalse(MiningHold.flatPlant(-1.0F));
    }

    @Test
    void usableStaminaRespectsTheArmorFloorAndAbsorption() {
        assertEquals(20, MiningHold.usable(20, 0, 0));
        assertEquals(4, MiningHold.usable(10, 6, 0));
        assertEquals(0, MiningHold.usable(4, 6, 0));
        assertEquals(3, MiningHold.usable(4, 6, 3));
        assertEquals(0, MiningHold.usable(0, 0, 0));
    }

    @Test
    void fatigueFollowsUsableStaminaNotTheRawFeatherCount() {
        // Weight 8 armor: 8 feathers is an empty usable pool. The raw count (8) is above the threshold 3.
        assertTrue(MiningHold.fatigueDue(MiningHold.usable(8, 8, 0), 3));
        assertTrue(MiningHold.fatigueDue(MiningHold.usable(11, 8, 0), 3));
        assertFalse(MiningHold.fatigueDue(MiningHold.usable(12, 8, 0), 3));
        // Unarmored: the same threshold of 3.
        assertTrue(MiningHold.fatigueDue(MiningHold.usable(3, 0, 0), 3));
        assertFalse(MiningHold.fatigueDue(MiningHold.usable(4, 0, 0), 3));
    }
}
