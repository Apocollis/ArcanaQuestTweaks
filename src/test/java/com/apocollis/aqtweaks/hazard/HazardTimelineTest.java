package com.apocollis.aqtweaks.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HazardTimelineTest {
    private static final HazardTiming T = new HazardTiming(2000, 4000, 600, 1200, 300, 100, 100);

    @Test
    void firstUpdateDrawsAnEventAfterTheGap() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        List<HazardEvent> created = t.update(5000, T);
        assertEquals(1, created.size());
        HazardEvent e = t.event();
        assertTrue(e.start >= 5000 + 2000 && e.start <= 5000 + 4000);
        assertTrue(e.end - e.start >= 600 && e.end - e.start <= 1200);
        assertEquals(e.start - 300, e.warningStart);
        assertTrue(t.update(5001, T).isEmpty());
    }

    @Test
    void sameKeyAndIndexGiveSameLengths() {
        HazardTimeline a = new HazardTimeline(0, 42L);
        HazardTimeline b = new HazardTimeline(0, 42L);
        a.update(0, T);
        b.update(0, T);
        assertEquals(a.event().start, b.event().start);
        assertEquals(a.event().end, b.event().end);
        assertEquals(a.event().seed, b.event().seed);
    }

    @Test
    void nextEventStartsFromTheEndOfTheLast() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        t.update(0, T);
        HazardEvent first = t.event();
        List<HazardEvent> created = t.update(first.end, T);
        assertEquals(1, created.size());
        assertTrue(t.event().id == first.id + 1);
        assertTrue(t.event().start >= first.end + 2000);
    }

    @Test
    void longAbsenceDrawsFreshGapFromNow() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        t.update(0, T);
        HazardEvent first = t.event();
        long later = first.end + 1_000_000L;
        t.update(later, T);
        assertTrue(t.event().start >= later + 2000);
    }

    @Test
    void triggerWarnsThenStarts() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        t.update(0, T);
        HazardEvent e = t.trigger(1000, 0, false, T);
        assertEquals(1000, e.warningStart);
        assertEquals(1300, e.start);
        assertEquals(e.id, t.event().id);
        HazardEvent s = t.trigger(2000, 400, true, T);
        assertEquals(2000, s.start);
        assertEquals(2400, s.end);
        assertEquals(2000, s.warningStart);
    }

    @Test
    void clearRampsDownAnActiveEvent() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        t.trigger(0, 5000, true, T);
        List<HazardEvent> out = t.clear(1000, T);
        assertEquals(1, out.size());
        assertEquals(1100, t.event().end);
        assertEquals(100, t.event().rampDown);
        assertTrue(t.event().strengthAt(1000) > 0.99F);
        assertEquals(0.0F, t.event().strengthAt(1100));
    }

    @Test
    void clearCancelsAnUpcomingEventAndDrawsAFreshGap() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        HazardEvent e = t.trigger(0, 0, false, T);
        List<HazardEvent> out = t.clear(100, T);
        assertEquals(1, out.size());
        assertTrue(t.event().start >= 100 + 2000);
        assertTrue(t.event().id > e.id);
    }

    @Test
    void clearWithNothingScheduledDoesNothing() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        assertTrue(t.clear(0, T).isEmpty());
        assertNull(t.event());
    }

    @Test
    void configChangeAffectsOnlyTheNextDraw() {
        HazardTimeline t = new HazardTimeline(0, 42L);
        t.update(0, T);
        HazardEvent first = t.event();
        HazardTiming other = new HazardTiming(100, 200, 50, 60, 10, 5, 5);
        assertTrue(t.update(1, other).isEmpty());
        assertEquals(first.start, t.event().start);
        t.update(first.end, other);
        assertNotNull(t.event());
        assertTrue(t.event().start - first.end <= 200);
    }

    @Test
    void restoredEventIsKept() {
        HazardTimeline a = new HazardTimeline(0, 42L);
        a.update(0, T);
        HazardTimeline b = new HazardTimeline(0, 42L);
        b.restore(a.index(), a.lastEnd(), a.event());
        assertTrue(b.update(10, T).isEmpty());
        assertEquals(a.event().start, b.event().start);
    }
}
