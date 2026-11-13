package com.apocollis.aqtweaks.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StormScheduleTest {

    private static StormSchedule schedule(long key) {
        // clear 360..1200 s, storm 240..420 s, ramp and warning 600 ticks (Atum timing defaults)
        return new StormSchedule(key, 360 * 20L, 1200 * 20L, 240 * 20L, 420 * 20L, 600, 600);
    }

    @Test
    void segmentLengthsStayInRange() {
        StormSchedule s = schedule(12345L);
        for (long i = 0; i < 500; i++) {
            long clear = s.clearLength(i);
            long storm = s.stormLength(i);
            assertTrue(clear >= 360 * 20L && clear <= 1200 * 20L, "clear " + clear);
            assertTrue(storm >= 240 * 20L && storm <= 420 * 20L, "storm " + storm);
        }
    }

    @Test
    void sameKeyAgreesAcrossInstances() {
        StormSchedule a = schedule(99L);
        StormSchedule b = schedule(99L);
        for (long t = 0; t < 400_000L; t += 997L) {
            StormSchedule.State sa = a.at(t);
            StormSchedule.State sb = b.at(t);
            assertEquals(sa.strength, sb.strength);
            assertEquals(sa.warning, sb.warning);
            assertEquals(sa.storming, sb.storming);
        }
    }

    @Test
    void differentKeysDiffer() {
        assertTrue(schedule(1L).clearLength(0) != schedule(2L).clearLength(0)
                || schedule(1L).clearLength(1) != schedule(2L).clearLength(1));
    }

    @Test
    void phasesAndRampsAreConsistent() {
        StormSchedule s = schedule(7L);
        long stormStart = s.clearLength(0);
        long stormEnd = stormStart + s.stormLength(0);

        StormSchedule.State clear = s.at(0);
        assertFalse(clear.storming);
        assertEquals(0.0F, clear.strength);
        assertEquals(0.0F, clear.warning);
        assertEquals(stormStart, clear.ticksToNextStorm);

        StormSchedule.State warn = s.at(stormStart - 300);
        assertFalse(warn.storming);
        assertEquals(0.0F, warn.strength);
        assertEquals(0.5F, warn.warning, 1.0E-4F);

        StormSchedule.State start = s.at(stormStart);
        assertTrue(start.storming);
        assertEquals(0.0F, start.strength, 1.0E-6F);
        assertEquals(0.0F, start.warning);

        assertEquals(1.0F, s.at(stormStart + 600).strength, 1.0E-6F);
        assertEquals(1.0F, s.at(stormStart + s.stormLength(0) / 2).strength, 1.0E-6F);
        assertTrue(s.at(stormStart + 300).strength > 0.0F && s.at(stormStart + 300).strength < 1.0F);
        assertEquals(0.0F, s.at(stormEnd - 1).strength, 0.01F);

        StormSchedule.State after = s.at(stormEnd);
        assertFalse(after.storming);
        assertEquals(0.0F, after.strength);
    }

    @Test
    void movingBackwardsRecomputes() {
        StormSchedule s = schedule(5L);
        StormSchedule.State late = s.at(3_000_000L);
        StormSchedule.State early = s.at(10L);
        assertEquals(0L, early.segStart);
        assertTrue(late.segStart > 0L);
        assertEquals(late.stormStart, s.at(3_000_000L).stormStart);
    }

    @Test
    void sameParamsDetectsConfigEdits() {
        StormSchedule s = schedule(3L);
        assertTrue(s.sameParams(3L, 360 * 20L, 1200 * 20L, 240 * 20L, 420 * 20L, 600, 600));
        assertFalse(s.sameParams(3L, 361 * 20L, 1200 * 20L, 240 * 20L, 420 * 20L, 600, 600));
        assertFalse(s.sameParams(4L, 360 * 20L, 1200 * 20L, 240 * 20L, 420 * 20L, 600, 600));
    }

    @Test
    void shortStormPeaksBelowOne() {
        StormSchedule s = new StormSchedule(1L, 1000L, 1000L, 600L, 600L, 6000, 0);
        StormSchedule.State mid = s.at(1000L + 300L);
        assertTrue(mid.strength < 1.0F && mid.strength > 0.0F);
    }
}
