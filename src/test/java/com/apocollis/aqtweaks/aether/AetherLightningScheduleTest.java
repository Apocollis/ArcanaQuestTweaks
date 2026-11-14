package com.apocollis.aqtweaks.aether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class AetherLightningScheduleTest {
    @Test
    void firstSalvoStrikesAtTheEndOfTheWarning() {
        long[] waves = AetherLightningSchedule.planWaves(1000L, 1200L, 1200L + 400L, 35, 6, new Random(1L));
        assertEquals(1200L, waves[0]);
    }

    @Test
    void enteredActiveStartsOneTelegraphLater() {
        long[] waves = AetherLightningSchedule.planWaves(5000L, 5000L, 5000L + 400L, 35, 5, new Random(1L));
        assertEquals(5035L, waves[0]);
    }

    @Test
    void salvosAreOrderedSpacedAndLeaveATail() {
        for (int seed = 0; seed < 200; seed++) {
            for (int storm : new int[] {360, 420, 480}) {
                for (int count = 5; count <= 6; count++) {
                    long start = 10000L;
                    long end = start + storm;
                    long[] waves = AetherLightningSchedule.planWaves(start - 200L, start, end, 35, count,
                            new Random(seed));
                    assertEquals(count, waves.length);
                    for (int i = 1; i < waves.length; i++) {
                        assertTrue(waves[i] - waves[i - 1] >= AetherLightningSchedule.MIN_GAP);
                    }
                    long last = waves[waves.length - 1];
                    assertTrue(last <= end - AetherLightningSchedule.TAIL_TICKS);
                }
            }
        }
    }

    @Test
    void eighteenSecondStormSpreadsAboutThreeSecondsApart() {
        long[] waves = AetherLightningSchedule.planWaves(0L, 200L, 200L + 360L, 35, 6, new Random(9L));
        double span = (waves[5] - waves[0]) / 20.0D;
        assertEquals(15.0D, span, 0.01D);
    }

    @Test
    void singleSalvoIsJustTheFirst() {
        long[] waves = AetherLightningSchedule.planWaves(0L, 100L, 500L, 35, 1, new Random(2L));
        assertEquals(1, waves.length);
        assertEquals(100L, waves[0]);
    }
}
