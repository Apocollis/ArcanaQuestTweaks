package com.apocollis.aqtweaks.starter.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RevealTimelineTest {

    private static void tick(RevealTimeline t, int n) {
        for (int i = 0; i < n; i++) {
            t.tick();
        }
    }

    @Test
    void fadesInHoldsFadesOutAndFinishes() {
        RevealTimeline t = new RevealTimeline(10, 240);
        assertEquals(RevealTimeline.Stage.FADE_IN, t.stage());
        assertEquals(0f, t.alpha(0f), 1e-4);
        tick(t, 5);
        assertEquals(RevealTimeline.Stage.FADE_IN, t.stage());
        assertEquals(0.5f, t.alpha(0f), 1e-4);
        tick(t, 5);
        assertEquals(RevealTimeline.Stage.HOLD, t.stage());
        assertEquals(1f, t.alpha(0f), 1e-4);
        tick(t, 239);
        assertEquals(RevealTimeline.Stage.HOLD, t.stage());
        tick(t, 1);
        assertEquals(RevealTimeline.Stage.FADE_OUT, t.stage());
        tick(t, 9);
        assertEquals(RevealTimeline.Stage.FADE_OUT, t.stage());
        assertFalse(t.done());
        tick(t, 1);
        assertTrue(t.done());
        assertEquals(0f, t.alpha(0f), 1e-4);
    }

    @Test
    void continueSkipsTheHold() {
        RevealTimeline t = new RevealTimeline(10, 240);
        tick(t, 30);
        assertEquals(RevealTimeline.Stage.HOLD, t.stage());
        t.continueClicked();
        assertEquals(RevealTimeline.Stage.FADE_OUT, t.stage());
        tick(t, 10);
        assertTrue(t.done());
    }

    @Test
    void continueDuringFadeInWaitsForTheFadeToFinish() {
        RevealTimeline t = new RevealTimeline(10, 240);
        tick(t, 3);
        t.continueClicked();
        assertEquals(RevealTimeline.Stage.FADE_IN, t.stage());
        tick(t, 7);
        assertEquals(RevealTimeline.Stage.FADE_OUT, t.stage());
        tick(t, 10);
        assertTrue(t.done());
    }

    @Test
    void doneStaysDone() {
        RevealTimeline t = new RevealTimeline(5, 0);
        tick(t, 100);
        assertTrue(t.done());
        int ticks = t.ticks();
        tick(t, 10);
        assertEquals(ticks, t.ticks());
    }
}
