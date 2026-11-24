package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CloudAvoidanceTest {

    @Test
    void aSpotInsideAnOwnCloudIsRejectedAndOneOutsideIsFree() {
        CloudAvoidance a = new CloudAvoidance(0.75D, 6.0D).addOwn(new CloudAvoidance.Zone(10, 64, 10, 3.0D));
        assertEquals(Double.POSITIVE_INFINITY, a.penalty(11, 64, 10));
        assertEquals(0.0D, a.penalty(20, 64, 10));
    }

    @Test
    void theMarginGrowsTheZone() {
        CloudAvoidance tight = new CloudAvoidance(0.0D, 6.0D).addOwn(new CloudAvoidance.Zone(0, 64, 0, 3.0D));
        CloudAvoidance wide = new CloudAvoidance(1.0D, 6.0D).addOwn(new CloudAvoidance.Zone(0, 64, 0, 3.0D));
        assertEquals(0.0D, tight.penalty(3.5D, 64, 0));
        assertEquals(Double.POSITIVE_INFINITY, wide.penalty(3.5D, 64, 0));
    }

    @Test
    void aSpotFarAboveOrBelowACloudIsOutsideIt() {
        CloudAvoidance a = new CloudAvoidance(0.75D, 6.0D).addOwn(new CloudAvoidance.Zone(0, 64, 0, 3.0D));
        assertEquals(Double.POSITIVE_INFINITY, a.penalty(0, 66, 0));
        assertEquals(0.0D, a.penalty(0, 72, 0));
        assertEquals(0.0D, a.penalty(0, 56, 0));
    }

    @Test
    void anEnemyCloudIsASoftCostThatAddsUp() {
        CloudAvoidance a = new CloudAvoidance(0.5D, 6.0D)
                .addEnemy(new CloudAvoidance.Zone(0, 64, 0, 4.0D))
                .addEnemy(new CloudAvoidance.Zone(2, 64, 0, 4.0D));
        assertEquals(12.0D, a.penalty(1, 64, 0), 1e-9);
        assertEquals(6.0D, a.penalty(-4, 64, 0), 1e-9);
        assertEquals(0.0D, a.penalty(30, 64, 0), 1e-9);
    }

    @Test
    void anOwnCloudBeatsAnEnemyCloudAtTheSameSpot() {
        CloudAvoidance a = new CloudAvoidance(0.5D, 6.0D)
                .addEnemy(new CloudAvoidance.Zone(0, 64, 0, 4.0D))
                .addOwn(new CloudAvoidance.Zone(0, 64, 0, 4.0D));
        assertEquals(Double.POSITIVE_INFINITY, a.penalty(0, 64, 0));
    }

    @Test
    void withEveryCandidateInAnEnemyCloudTheLeastBadSpotStillWins() {
        // The whole ring around the target is covered by one enemy cloud; two spots are also in a second one.
        BlinkPicker.Request r = new BlinkPicker.Request();
        r.mode = BlinkPicker.Mode.REPOSITION_BAND;
        r.fromX = 0;
        r.fromY = 64;
        r.fromZ = 0;
        r.centerX = 0;
        r.centerY = 64;
        r.centerZ = 0;
        r.minRadius = 8.0D;
        r.maxRadius = 14.0D;
        r.maxTravel = 40.0D;
        r.samples = 24;
        CloudAvoidance a = new CloudAvoidance(0.5D, 6.0D)
                .addEnemy(new CloudAvoidance.Zone(0, 64, 0, 30.0D))
                .addEnemy(new CloudAvoidance.Zone(11, 64, 0, 4.0D));
        r.penalties.add(a);
        List<BlinkPicker.Candidate> out = BlinkPicker.rank(r, new Random(1L), 0);
        assertEquals(24, out.size(), "soft costs never reject");
        BlinkPicker.Candidate best = out.get(0);
        assertEquals(6.0D, a.penalty(best.x, best.y, best.z), 1e-9, "the best spot is in only one enemy cloud");
    }

    @Test
    void aBlinkRingNeverOffersASpotInsideAnOwnCloud() {
        BlinkPicker.Request r = new BlinkPicker.Request();
        r.mode = BlinkPicker.Mode.REPOSITION_BAND;
        r.centerX = 0;
        r.centerZ = 0;
        r.minRadius = 8.0D;
        r.maxRadius = 14.0D;
        r.maxTravel = 60.0D;
        r.samples = 64;
        CloudAvoidance a = new CloudAvoidance(0.75D, 6.0D)
                .addOwn(new CloudAvoidance.Zone(0, 0, 10, 5.0D))
                .addOwn(new CloudAvoidance.Zone(-9, 0, -3, 4.0D));
        r.penalties.add(a);
        List<BlinkPicker.Candidate> out = BlinkPicker.rank(r, new Random(7L), 0);
        assertFalse(out.isEmpty());
        assertTrue(out.size() < 64, "some points were inside a cloud");
        for (BlinkPicker.Candidate c : out) {
            assertEquals(0.0D, a.penalty(c.x, c.y, c.z), 0.0D);
        }
    }

    @Test
    void countsReportWhatWasAdded() {
        CloudAvoidance a = new CloudAvoidance(0.5D, 6.0D).addOwn(new CloudAvoidance.Zone(0, 0, 0, 1.0D))
                .addEnemy(new CloudAvoidance.Zone(0, 0, 0, 1.0D)).addEnemy(new CloudAvoidance.Zone(5, 0, 0, 1.0D));
        assertEquals(1, a.ownCount());
        assertEquals(2, a.enemyCount());
    }
}
