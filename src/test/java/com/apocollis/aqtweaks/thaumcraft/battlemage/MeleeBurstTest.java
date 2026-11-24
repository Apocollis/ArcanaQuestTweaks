package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class MeleeBurstTest {

    @Test
    void planStaysInsideTheBounds() {
        Random r = new Random(5);
        boolean two = false;
        boolean three = false;
        for (int i = 0; i < 200; i++) {
            int n = MeleeBurst.plan(2, 3, r);
            assertTrue(n == 2 || n == 3);
            two |= n == 2;
            three |= n == 3;
        }
        assertTrue(two && three);
        assertEquals(1, MeleeBurst.plan(0, 0, r));
    }

    @Test
    void aBurstAttacksOnTheIntervalThenEndsAndCoolsDown() {
        MeleeBurst b = new MeleeBurst();
        assertTrue(b.canStart(0));
        b.start(0, 3);
        assertTrue(b.active());
        assertFalse(b.canStart(0));
        assertTrue(b.attackDue(0));
        b.onAttack(0, 14);
        assertFalse(b.attackDue(10));
        assertTrue(b.attackDue(14));
        b.onAttack(14, 14);
        assertFalse(b.finished(20, 60));
        b.onAttack(28, 14);
        assertTrue(b.finished(30, 60));
        b.end(30, 100);
        assertFalse(b.active());
        assertFalse(b.canStart(129));
        assertTrue(b.canStart(130));
    }

    @Test
    void aBurstEndsAtTheTimeLimitEvenWithoutAttacks() {
        MeleeBurst b = new MeleeBurst();
        b.start(100, 3);
        assertFalse(b.finished(159, 60));
        assertTrue(b.finished(160, 60));
    }

    @Test
    void abortHasNoCooldown() {
        MeleeBurst b = new MeleeBurst();
        b.start(0, 2);
        b.abort();
        assertTrue(b.canStart(1));
    }
}
