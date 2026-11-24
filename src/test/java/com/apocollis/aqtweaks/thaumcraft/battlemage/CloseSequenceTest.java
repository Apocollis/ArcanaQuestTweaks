package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CloseSequenceTest {

    private static CloseSequence.Input in(long now, double dist) {
        CloseSequence.Input i = new CloseSequence.Input();
        i.now = now;
        i.dist = dist;
        i.knockBackReady = true;
        i.blinkReady = true;
        return i;
    }

    @Test
    void aFarTargetDoesNothing() {
        CloseSequence s = new CloseSequence();
        assertEquals(CloseSequence.Action.NONE, s.step(in(0, 12.0D), new Random(1L)));
        assertFalse(s.active());
    }

    @Test
    void withinMeleeRangeItMeleesThenKnocksBackThenBlinks() {
        CloseSequence s = new CloseSequence();
        Random rnd = new Random(2L);
        CloseSequence.Input i = in(0, 2.0D);
        assertEquals(CloseSequence.Action.MELEE, s.step(i, rnd));
        assertTrue(s.burstSize() >= 1 && s.burstSize() <= 2);

        // the burst is running: nothing else happens
        i = in(5, 2.0D);
        i.burstActive = true;
        assertEquals(CloseSequence.Action.NONE, s.step(i, rnd));
        assertEquals(CloseSequence.State.MELEE, s.state());

        // the burst ended: Knock Back
        i = in(30, 2.0D);
        assertEquals(CloseSequence.Action.KNOCK_BACK, s.step(i, rnd));

        // then the blink
        i = in(31, 4.5D);
        assertEquals(CloseSequence.Action.BLINK, s.step(i, rnd));
        s.blinked();
        assertFalse(s.active());
    }

    @Test
    void knockBackIsSkippedWhenNotReadyOrOutOfReach() {
        CloseSequence s = new CloseSequence();
        Random rnd = new Random(3L);
        s.step(in(0, 2.0D), rnd); // MELEE
        CloseSequence.Input i = in(30, 2.0D);
        i.knockBackReady = false;
        assertEquals(CloseSequence.Action.BLINK, s.step(i, rnd));

        CloseSequence s2 = new CloseSequence();
        s2.step(in(0, 2.0D), rnd);
        CloseSequence.Input far = in(30, 3.5D);
        assertEquals(CloseSequence.Action.BLINK, s2.step(far, rnd), "3.5 blocks is outside Knock Back's 3 block reach");
    }

    @Test
    void aTargetBetweenThreeAndSixBlocksGoesStraightToTheBlink() {
        CloseSequence s = new CloseSequence();
        assertEquals(CloseSequence.Action.BLINK, s.step(in(0, 4.5D), new Random(4L)));
        assertEquals(CloseSequence.State.BLINK, s.state());
    }

    @Test
    void withBlinkOnCooldownItRetreatsAndBlinksWhenItIsReady() {
        CloseSequence s = new CloseSequence();
        Random rnd = new Random(5L);
        CloseSequence.Input i = in(0, 4.0D);
        i.blinkReady = false;
        assertEquals(CloseSequence.Action.RETREAT, s.step(i, rnd));
        i = in(10, 4.5D);
        i.blinkReady = false;
        assertEquals(CloseSequence.Action.RETREAT, s.step(i, rnd));
        i = in(20, 5.0D);
        assertEquals(CloseSequence.Action.BLINK, s.step(i, rnd));
    }

    @Test
    void leavingTheCloseRingEndsTheSequence() {
        CloseSequence s = new CloseSequence();
        Random rnd = new Random(6L);
        s.step(in(0, 2.0D), rnd);
        CloseSequence.Input i = in(5, 7.0D);
        i.burstActive = true;
        assertEquals(CloseSequence.Action.NONE, s.step(i, rnd));
        assertFalse(s.active());
    }

    @Test
    void aSequenceThatDragsOnStartsOver() {
        CloseSequence s = new CloseSequence();
        Random rnd = new Random(7L);
        CloseSequence.Input i = in(0, 4.0D);
        i.blinkReady = false;
        assertEquals(CloseSequence.Action.RETREAT, s.step(i, rnd));
        i = in(160, 2.0D);
        i.blinkReady = false;
        assertEquals(CloseSequence.Action.MELEE, s.step(i, rnd), "after maxTicks it re-enters from the top");
    }

    @Test
    void theBurstSizeStaysInTheConfiguredRange() {
        for (long seed = 0; seed < 50; seed++) {
            CloseSequence s = new CloseSequence();
            CloseSequence.Input i = in(0, 1.5D);
            i.meleeMin = 1;
            i.meleeMax = 2;
            s.step(i, new Random(seed));
            assertTrue(s.burstSize() == 1 || s.burstSize() == 2);
        }
    }
}
