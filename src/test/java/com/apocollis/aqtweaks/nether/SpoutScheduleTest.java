package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SpoutScheduleTest {
    @Test
    void countStaggerAndWindowHold() {
        Random r = new Random(5L);
        for (int run = 0; run < 200; run++) {
            long[] t = SpoutSchedule.draw(r, 3, 6, 60, 40, 100, 600L);
            assertTrue(t.length <= 6);
            for (int i = 0; i < t.length; i++) {
                assertTrue(t[i] >= 0 && t[i] < 600L);
                if (i == 0) assertTrue(t[i] <= 60);
                else {
                    long gap = t[i] - t[i - 1];
                    assertTrue(gap >= 40 && gap <= 100);
                }
            }
        }
    }

    @Test
    void eruptionsPastTheWindowAreDropped() {
        Random r = new Random(1L);
        long[] t = SpoutSchedule.draw(r, 6, 6, 0, 100, 100, 250L);
        assertTrue(t.length == 3);
    }

    @Test
    void newDefaultsFitTheFortyFiveSecondWindow() {
        // 10 to 16 eruptions, 1.5 to 3.5 s apart (30 to 70 ticks), first within 3 s, window 45 s.
        Random r = new Random(9L);
        int total = 0;
        for (int run = 0; run < 500; run++) {
            long[] t = SpoutSchedule.draw(r, 10, 16, 60, 30, 70, 900L);
            assertTrue(t.length >= 8 && t.length <= 16, "count " + t.length);
            for (long at : t) {
                assertTrue(at >= 0 && at < 900L);
            }
            total += t.length;
        }
        // On average most of the requested eruptions fit (a worst-case draw ends a few early).
        assertTrue(total / 500.0D > 11.0D);
    }
}
