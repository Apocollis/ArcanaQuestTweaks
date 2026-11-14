package com.apocollis.aqtweaks.nether;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class SplashAimTest {
    @Test
    void distanceStaysInRange() {
        Random rand = new Random(7L);
        for (int i = 0; i < 5000; i++) {
            double d = SplashAim.distance(rand);
            assertTrue(d >= SplashAim.MIN_DISTANCE && d < SplashAim.MAX_DISTANCE);
        }
    }

    @Test
    void mostThrowsFavourThePlayerDirection() {
        Random rand = new Random(11L);
        double toward = 1.0D;
        int inside = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            if (SplashAim.inArc(SplashAim.angle(rand, toward), toward)) inside++;
        }
        // 60% aimed, plus the share of the 40% free throws that happen to land inside the 150 degree arc.
        double expected = SplashAim.BIAS + (1.0D - SplashAim.BIAS) * (2.0D * SplashAim.ARC / (Math.PI * 2.0D));
        assertEquals(expected, (double) inside / n, 0.02D);
    }

    @Test
    void noPlayerMeansAnyDirection() {
        Random rand = new Random(3L);
        int inside = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            if (SplashAim.inArc(SplashAim.angle(rand, Double.NaN), 0.0D)) inside++;
        }
        assertEquals(2.0D * SplashAim.ARC / (Math.PI * 2.0D), (double) inside / n, 0.02D);
    }

    @Test
    void arcWrapsAroundTwoPi() {
        assertTrue(SplashAim.inArc(0.1D, Math.PI * 2.0D - 0.1D));
        assertTrue(!SplashAim.inArc(Math.PI, 0.0D));
    }
}
