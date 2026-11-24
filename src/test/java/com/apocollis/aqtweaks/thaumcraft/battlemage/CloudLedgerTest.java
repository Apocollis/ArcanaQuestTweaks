package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CloudLedgerTest {

    private static List<UUID> ids(UUID... u) {
        return new ArrayList<>(Arrays.asList(u));
    }

    @Test
    void newCloudsAreCreditedToTheCastInProgress() {
        CloudLedger l = new CloudLedger();
        UUID c1 = UUID.randomUUID();
        l.expect("tempest", 10L);
        l.observe(ids(c1), 1L);
        assertTrue(l.hasLive("tempest"));
        assertFalse(l.hasLive("entropic_cloud"));
        assertEquals(1, l.liveCasts());
    }

    @Test
    void aCloudThatWasAlreadyOutIsNotCreditedToTheNextCast() {
        CloudLedger l = new CloudLedger();
        UUID old = UUID.randomUUID();
        l.expect(null, 0L);
        l.observe(ids(old), 0L); // seed
        l.expect("tempest", 10L);
        l.observe(ids(old), 1L);
        assertFalse(l.hasLive("tempest"), "the old cloud is not from this cast");
        UUID fresh = UUID.randomUUID();
        l.observe(ids(old, fresh), 2L);
        assertTrue(l.hasLive("tempest"));
    }

    @Test
    void severalCloudsFromOneCastCountAsOneLiveCast() {
        CloudLedger l = new CloudLedger();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        l.expect("fire_and_ice", 10L);
        l.observe(ids(a, b, c), 1L);
        assertEquals(1, l.liveCasts());
        assertTrue(l.hasLive("fire_and_ice"));
        // one cloud expires: the cast is still live; all gone: free again.
        l.observe(ids(b, c), 5L);
        assertTrue(l.hasLive("fire_and_ice"));
        l.observe(ids(), 9L);
        assertFalse(l.hasLive("fire_and_ice"));
        assertEquals(0, l.liveCasts());
    }

    @Test
    void fiveFociCanEachHaveALiveCastAtTheSameTime() {
        CloudLedger l = new CloudLedger();
        List<UUID> alive = new ArrayList<>();
        String[] foci = {"entropic_cloud", "fire_and_ice", "tempest", "sands_of_atum", "aurorian_winds"};
        long t = 0;
        for (String f : foci) {
            l.expect(null, 0L);
            l.observe(alive, t);
            l.expect(f, t + 10);
            alive.add(UUID.randomUUID());
            l.observe(alive, t);
            t += 30;
        }
        assertEquals(5, l.liveCasts());
        for (String f : foci) {
            assertTrue(l.hasLive(f), f);
        }
    }

    @Test
    void aCloudThatSpawnsLateIsStillCreditedInsideTheWindowButNotAfter() {
        CloudLedger l = new CloudLedger();
        l.expect("tempest", 10L);
        l.observe(ids(), 2L);
        UUID late = UUID.randomUUID();
        l.observe(ids(late), 8L);
        assertTrue(l.hasLive("tempest"));

        CloudLedger l2 = new CloudLedger();
        l2.expect("tempest", 10L);
        l2.observe(ids(), 2L);
        l2.observe(ids(UUID.randomUUID()), 11L);
        assertFalse(l2.hasLive("tempest"), "outside the window the cloud is unattributed");
    }

    @Test
    void clearForgetsEverything() {
        CloudLedger l = new CloudLedger();
        l.expect("tempest", 10L);
        l.observe(ids(UUID.randomUUID()), 1L);
        l.clear();
        assertEquals(0, l.liveCasts());
    }
}
