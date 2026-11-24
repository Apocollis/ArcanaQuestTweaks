package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BlinkPickerTest {

    private static BlinkPicker.Request ally(double mageX, double mageZ, double allyX, double allyZ) {
        BlinkPicker.Request r = new BlinkPicker.Request();
        r.mode = BlinkPicker.Mode.TOWARD_ALLY;
        r.fromX = mageX;
        r.fromZ = mageZ;
        r.centerX = allyX;
        r.centerZ = allyZ;
        r.minRadius = 1.0D;
        r.maxRadius = 2.0D;
        r.samples = 16;
        return r;
    }

    private static double ring(BlinkPicker.Candidate c, double cx, double cz) {
        return Math.sqrt((c.x - cx) * (c.x - cx) + (c.z - cz) * (c.z - cz));
    }

    @Test
    void everyCandidateLiesOnTheOneToTwoBlockRingAroundTheAlly() {
        BlinkPicker.Request r = ally(0, 0, 10, 4);
        List<BlinkPicker.Candidate> out = BlinkPicker.rank(r, new Random(1L), 0);
        assertEquals(16, out.size());
        for (BlinkPicker.Candidate c : out) {
            double d = ring(c, 10, 4);
            assertTrue(d >= 1.0D - 1e-9 && d <= 2.0D + 1e-9, "radius " + d);
        }
    }

    @Test
    void theResultIsSortedBestFirstAndLimited() {
        BlinkPicker.Request r = ally(0, 0, 10, 4);
        List<BlinkPicker.Candidate> out = BlinkPicker.rank(r, new Random(2L), 3);
        assertEquals(3, out.size());
        assertTrue(out.get(0).score >= out.get(1).score && out.get(1).score >= out.get(2).score);
    }

    @Test
    void withAFixedRadiusTheShortestHopWins() {
        BlinkPicker.Request r = ally(0, 0, 20, 0);
        r.minRadius = 1.5D;
        r.maxRadius = 1.5D;
        BlinkPicker.Candidate best = BlinkPicker.rank(r, new Random(3L), 1).get(0);
        assertTrue(best.x < 20.0D, "a spot on the mage's side of the ally beats the far side");
    }

    @Test
    void aSpotNearThePreferredRadiusBeatsOneAtTheEdgeOfTheRing() {
        BlinkPicker.Request r = ally(0, 0, 0, 0);
        r.fromX = 0.0D;
        r.fromZ = 0.0D;
        r.preferRadius = 1.5D;
        List<BlinkPicker.Candidate> out = BlinkPicker.rank(r, new Random(3L), 0);
        double bestRadius = ring(out.get(0), 0, 0);
        assertTrue(Math.abs(bestRadius - 1.5D) <= 0.4D, "best radius " + bestRadius);
    }

    @Test
    void aSpotBeyondTheTravelCapIsDropped() {
        BlinkPicker.Request r = ally(0, 0, 40, 0);
        r.maxTravel = 24.0D;
        assertTrue(BlinkPicker.rank(r, new Random(4L), 0).isEmpty());
        r.maxTravel = 60.0D;
        assertFalse(BlinkPicker.rank(r, new Random(4L), 0).isEmpty());
    }

    @Test
    void aSoftPenaltyLowersTheScoreAndInfinityRejects() {
        BlinkPicker.Request base = ally(0, 0, 10, 0);
        double plain = BlinkPicker.rank(base, new Random(5L), 0).get(0).score;

        BlinkPicker.Request soft = ally(0, 0, 10, 0);
        soft.penalties.add(new BlinkPicker.Penalty() {
            @Override
            public double penalty(double x, double y, double z) {
                return 5.0D;
            }
        });
        assertEquals(plain - 5.0D, BlinkPicker.rank(soft, new Random(5L), 0).get(0).score, 1e-9);

        BlinkPicker.Request hard = ally(0, 0, 10, 0);
        hard.penalties.add(new BlinkPicker.Penalty() {
            @Override
            public double penalty(double x, double y, double z) {
                return x < 10.0D ? Double.POSITIVE_INFINITY : 0.0D;
            }
        });
        for (BlinkPicker.Candidate c : BlinkPicker.rank(hard, new Random(5L), 0)) {
            assertTrue(c.x >= 10.0D, "points west of the ally are rejected");
        }
    }

    private static BlinkPicker.Request band(double mageX, double mageZ, double targetX, double targetZ) {
        BlinkPicker.Request r = new BlinkPicker.Request();
        r.mode = BlinkPicker.Mode.REPOSITION_BAND;
        r.fromX = mageX;
        r.fromZ = mageZ;
        r.centerX = targetX;
        r.centerZ = targetZ;
        r.minRadius = 8.0D;
        r.maxRadius = 14.0D;
        r.preferRadius = 11.0D;
        r.samples = 32;
        return r;
    }

    @Test
    void theBandModeLandsEightToFourteenBlocksFromTheTarget() {
        BlinkPicker.Request r = band(2, 1, 0, 0);
        List<BlinkPicker.Candidate> out = BlinkPicker.rank(r, new Random(11L), 0);
        assertEquals(32, out.size());
        for (BlinkPicker.Candidate c : out) {
            double d = ring(c, 0, 0);
            assertTrue(d >= 8.0D - 1e-9 && d <= 14.0D + 1e-9, "radius " + d);
        }
    }

    @Test
    void theBandModePrefersTheMiddleOfTheBandAndAShortHop() {
        BlinkPicker.Request r = band(0, 0, 0, 0);
        r.maxTravel = 40.0D;
        BlinkPicker.Candidate best = BlinkPicker.rank(r, new Random(12L), 1).get(0);
        double d = ring(best, 0, 0);
        assertTrue(Math.abs(d - 11.0D) <= 1.5D, "best radius " + d);

        // With the mage on the east side, the best spot is on its own side of the target (short hop).
        BlinkPicker.Request east = band(10, 0, 0, 0);
        east.preferRadius = Double.NaN;
        east.minRadius = 11.0D;
        east.maxRadius = 11.0D;
        east.samples = 64;
        BlinkPicker.Candidate near = BlinkPicker.rank(east, new Random(13L), 1).get(0);
        assertTrue(near.x > 0.0D, "x " + near.x);
    }

    @Test
    void theBandModeStillHonoursTheTravelCapAndRejectingPenalties() {
        BlinkPicker.Request r = band(0, 0, 40, 0);
        r.maxTravel = 24.0D;
        // every point of the 8 to 14 ring around (40,0) is 26 to 54 blocks from the mage: all dropped
        assertTrue(BlinkPicker.rank(r, new Random(14L), 0).isEmpty());

        BlinkPicker.Request hard = band(0, 0, 0, 0);
        hard.maxTravel = 40.0D;
        hard.penalties.add(new BlinkPicker.Penalty() {
            @Override
            public double penalty(double x, double y, double z) {
                return z > 0.0D ? Double.POSITIVE_INFINITY : 0.0D;
            }
        });
        for (BlinkPicker.Candidate c : BlinkPicker.rank(hard, new Random(15L), 0)) {
            assertTrue(c.z <= 0.0D);
        }
    }

    @Test
    void theSameSeedGivesTheSameRanking() {
        List<BlinkPicker.Candidate> a = BlinkPicker.rank(ally(0, 0, 10, 4), new Random(9L), 5);
        List<BlinkPicker.Candidate> b = BlinkPicker.rank(ally(0, 0, 10, 4), new Random(9L), 5);
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).x, b.get(i).x, 0.0D);
            assertEquals(a.get(i).z, b.get(i).z, 0.0D);
        }
    }
}
