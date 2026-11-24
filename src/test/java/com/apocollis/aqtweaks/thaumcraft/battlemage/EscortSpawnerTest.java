package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class EscortSpawnerTest {

    private static List<EscortDef> swarmEscort() {
        return Arrays.asList(new EscortDef("thaumcraft:cultistknight", 2), new EscortDef("thaumcraft:cultistcleric", 1),
                new EscortDef("crimsonrevelations:cultist_archer", 1));
    }

    private static final List<String> KNOWN_VALID = Arrays.asList("thaumcraft:cultistknight", "thaumcraft:cultistcleric",
            "crimsonrevelations:cultist_archer");

    @Test
    void everyEscortIdInTheBundledJsonIsWellFormedAndKnown() throws Exception {
        String json;
        try (java.io.InputStream in = EscortSpawnerTest.class.getResourceAsStream("/assets/aqtweaks/battlemage/cultist_battlemages.json")) {
            json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        LibraryData d = LibraryParser.parse(json);
        int seen = 0;
        for (VariantDef v : d.variants.values()) {
            for (EscortDef e : v.escort) {
                seen++;
                assertTrue(EscortSpawner.isValidId(e.entity), e.entity);
                assertTrue(KNOWN_VALID.contains(e.entity), "unknown escort id " + e.entity);
                assertFalse(e.entity.contains("cultist_knight") || e.entity.contains("cultist_cleric"), "the old underscore ids are wrong");
            }
        }
        assertTrue(seen >= 4 - 1, "the swarm mage escort entries were read");
    }

    @Test
    void unresolvedListsEachBadIdOnce() {
        List<EscortDef> defs = Arrays.asList(new EscortDef("thaumcraft:cultist_knight", 2), new EscortDef("thaumcraft:cultist_knight", 1),
                new EscortDef("thaumcraft:cultistcleric", 1), new EscortDef("Bad Id", 1));
        List<String> bad = EscortSpawner.unresolved(defs, id -> KNOWN_VALID.contains(id));
        assertEquals(Arrays.asList("thaumcraft:cultist_knight", "Bad Id"), bad);
        assertTrue(EscortSpawner.unresolved(swarmEscort(), id -> KNOWN_VALID.contains(id)).isEmpty());
        assertTrue(EscortSpawner.unresolved(null, id -> false).isEmpty());
        assertTrue(EscortSpawner.isValidId("thaumcraft:cultistknight"));
        assertFalse(EscortSpawner.isValidId(null));
        assertFalse(EscortSpawner.isValidId("cultistknight"));
    }

    @Test
    void theEscortExpandsInTheListedOrder() {
        assertEquals(Arrays.asList("thaumcraft:cultistknight", "thaumcraft:cultistknight", "thaumcraft:cultistcleric",
                "crimsonrevelations:cultist_archer"), EscortSpawner.expand(swarmEscort()));
        assertTrue(EscortSpawner.expand(null).isEmpty());
        assertTrue(EscortSpawner.expand(new ArrayList<EscortDef>()).isEmpty());
        assertTrue(EscortSpawner.expand(Arrays.asList(new EscortDef("x:y", 0), new EscortDef("x:z", -3))).isEmpty());
    }

    @Test
    void escortsDropTenTicksApartAfterTheFirstDelay() {
        assertEquals(15, EscortSpawner.spawnTick(0));
        assertEquals(25, EscortSpawner.spawnTick(1));
        assertEquals(45, EscortSpawner.spawnTick(3));
    }

    @Test
    void dueNowCountsOnlyTheEscortsWhoseTimeHasCome() {
        assertEquals(0, EscortSpawner.dueNow(14, 0, 4));
        assertEquals(1, EscortSpawner.dueNow(15, 0, 4));
        assertEquals(0, EscortSpawner.dueNow(15, 1, 4));
        assertEquals(2, EscortSpawner.dueNow(25, 0, 4));
        assertEquals(4, EscortSpawner.dueNow(70, 0, 4));
        assertEquals(0, EscortSpawner.dueNow(200, 4, 4), "never more than the total");
        assertEquals(1, EscortSpawner.dueNow(45, 3, 4), "a reload mid-arrival continues where it stopped");
    }

    @Test
    void theArrivalEndsWhenAllAreOutAndThePortalHadItsLife() {
        assertFalse(EscortSpawner.arrivalFinished(60, 4, 4));
        assertFalse(EscortSpawner.arrivalFinished(70, 3, 4));
        assertTrue(EscortSpawner.arrivalFinished(70, 4, 4));
    }

    @Test
    void portalAnglesCoverTheCircleOnceEach() {
        List<Double> a = EscortSpawner.portalAngles(new Random(7L), 12);
        assertEquals(12, a.size());
        assertEquals(12, new HashSet<>(a).size());
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double d : a) {
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        assertEquals(2.0D * Math.PI * 11.0D / 12.0D, max - min, 1e-9);
    }

    @Test
    void portalDistanceStaysInsideTheFourToSixRing() {
        Random rnd = new Random(11L);
        for (int i = 0; i < 500; i++) {
            double d = EscortSpawner.portalDistance(rnd);
            assertTrue(d >= 4.0D && d <= 6.0D, "distance " + d);
        }
    }
}
