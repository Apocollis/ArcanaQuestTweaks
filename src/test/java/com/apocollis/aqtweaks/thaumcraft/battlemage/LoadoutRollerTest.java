package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The roller runs on the real bundled library, so the shipped pools, tiers and budgets are what is tested. */
class LoadoutRollerTest {

    private static LibraryData lib() throws IOException {
        try (InputStream in = LoadoutRollerTest.class.getResourceAsStream("/assets/aqtweaks/battlemage/cultist_battlemages.json")) {
            assertNotNull(in);
            LibraryData d = LibraryParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(d.problems.isEmpty(), d.problems.toString());
            return d;
        }
    }

    private static Collection<FocusDef> pool(LibraryData d) {
        return d.foci.values();
    }

    private static boolean isDirect(FocusDef f) {
        return "bolt".equals(f.slot) || "projectile".equals(f.slot);
    }

    @Test
    void bundledTiersCarryTheDecidedNumbers() throws IOException {
        LibraryData d = lib();
        assertEquals(LibraryMigration.CURRENT, d.schemaVersion);
        assertEquals(80.0D, d.tier(1).health);
        assertEquals(1.0D, d.tier(1).armor);
        assertEquals(16.0D, d.tier(1).shieldMax);
        assertEquals(1.25D, d.tier(1).focusPower);
        assertEquals(120.0D, d.tier(2).health);
        assertEquals(3.0D, d.tier(2).armor);
        assertEquals(28.0D, d.tier(2).shieldMax);
        assertEquals(1.5D, d.tier(2).focusPower);
        assertEquals(160, d.tier(1).loadout.complexityBudget);
        assertEquals(250, d.tier(2).loadout.complexityBudget);
        assertEquals(0, d.tier(1).loadout.wildcards);
        assertEquals(2, d.tier(2).loadout.wildcards);
        assertEquals(50, d.tier(1).loadout.heavyComplexity);
        assertEquals(2, d.tier(1).loadout.maxHeavy);
        assertEquals(3, d.tier(2).loadout.maxHeavy);
        assertEquals(1, d.tier(2).loadout.minHeavyArea);
        assertEquals(1, d.tier(2).loadout.minHeavyOther);
        assertEquals(0.2D, d.tier(2).loadout.swarmChance);
        assertEquals(0.0D, d.tier(1).loadout.swarmChance);
    }

    private static int heavyCount(LibraryData d, List<String> ids, LoadoutRoller.Config cfg) {
        int n = 0;
        for (String id : ids) {
            if (!cfg.fixed.contains(id) && LoadoutRoller.isHeavy(d.focus(id), cfg)) {
                n++;
            }
        }
        return n;
    }

    @Test
    void heavyCapsHoldAndEveryMageHasAHeavyAreaFocus() throws IOException {
        LibraryData d = lib();
        LoadoutRoller.Config c1 = d.tier(1).loadout;
        LoadoutRoller.Config c2 = d.tier(2).loadout;
        Random rnd = new Random(20261118L);
        for (int n = 0; n < 5000; n++) {
            List<String> t1 = LoadoutRoller.roll(pool(d), c1, 0, rnd);
            assertTrue(heavyCount(d, t1, c1) <= 2, "tier 1 heavy cap broken: " + t1);
            assertTrue(hasHeavyArea(d, t1, c1), "tier 1 without a heavy area focus: " + t1);
            List<String> t2 = LoadoutRoller.roll(pool(d), c2, n % 3 == 0 ? -1 : 0, rnd);
            assertTrue(heavyCount(d, t2, c2) <= 3, "tier 2 heavy cap broken: " + t2);
            assertTrue(hasHeavyArea(d, t2, c2), "tier 2 without a heavy area focus: " + t2);
            assertTrue(hasHeavyNonAreaNonSwarm(d, t2, c2), "tier 2 without a heavy bolt or projectile: " + t2);
            assertEquals(10, t2.size(), t2.toString());
        }
    }

    private static boolean hasHeavyArea(LibraryData d, List<String> ids, LoadoutRoller.Config cfg) {
        for (String id : ids) {
            FocusDef f = d.focus(id);
            if ("aoe".equals(f.slot) && LoadoutRoller.isHeavy(f, cfg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHeavyNonAreaNonSwarm(LibraryData d, List<String> ids, LoadoutRoller.Config cfg) {
        for (String id : ids) {
            FocusDef f = d.focus(id);
            if (!cfg.fixed.contains(id) && !"aoe".equals(f.slot) && !cfg.swarmId.equals(id) && LoadoutRoller.isHeavy(f, cfg)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void swarmIsAboutAFifthOfTierTwoAndNeverSharesATierOneMageWithCurseBat() throws IOException {
        LibraryData d = lib();
        Random rnd = new Random(7L);
        int n = 8000;
        int swarm2 = 0;
        int swarm1 = 0;
        for (int k = 0; k < n; k++) {
            List<String> t2 = LoadoutRoller.roll(pool(d), d.tier(2).loadout, 0, rnd);
            if (t2.contains("spell_bat_swarm")) {
                swarm2++;
                assertTrue(t2.contains("curse_bat"), "a tier 2 Swarm mage keeps Curse Bat: " + t2);
            }
            List<String> t1 = LoadoutRoller.roll(pool(d), d.tier(1).loadout, 0, rnd);
            if (t1.contains("spell_bat_swarm")) {
                swarm1++;
                assertFalse(t1.contains("curse_bat"), "tier 1 carries one bat focus only: " + t1);
            }
        }
        double f2 = swarm2 / (double) n;
        assertTrue(f2 > 0.17D && f2 < 0.23D, "tier 2 Swarm rate " + f2);
        double f1 = swarm1 / (double) n;
        assertTrue(f1 > 0.15D && f1 < 0.35D, "tier 1 Swarm rate " + f1);
    }

    @Test
    void tierTwoAlwaysCarriesCurseBatBecauseSwarmLeavesTheNormalPools() throws IOException {
        LibraryData d = lib();
        Random rnd = new Random(99L);
        for (int k = 0; k < 3000; k++) {
            assertTrue(LoadoutRoller.roll(pool(d), d.tier(2).loadout, 0, rnd).contains("curse_bat"));
        }
    }

    @Test
    void tierOneHasEightFociAndNeverRollsATierTwoFocus() throws IOException {
        LibraryData d = lib();
        LoadoutRoller.Config cfg = d.tier(1).loadout;
        for (int seed = 0; seed < 600; seed++) {
            List<String> ids = LoadoutRoller.roll(pool(d), cfg, 0, new Random(seed));
            assertEquals(8, ids.size(), ids.toString());
            assertEquals(new HashSet<>(ids).size(), ids.size(), "duplicate in " + ids);
            for (String id : ids) {
                assertTrue(d.focus(id).minTier <= 1, "tier 2 focus " + id + " on a tier 1 mage");
            }
            for (String fixed : cfg.fixed) {
                assertTrue(ids.contains(fixed), fixed + " missing from " + ids);
            }
        }
    }

    @Test
    void tierTwoHasTenFoci() throws IOException {
        LibraryData d = lib();
        LoadoutRoller.Config cfg = d.tier(2).loadout;
        for (int seed = 0; seed < 600; seed++) {
            List<String> ids = LoadoutRoller.roll(pool(d), cfg, 0, new Random(seed));
            assertEquals(10, ids.size(), ids.toString());
            assertEquals(new HashSet<>(ids).size(), ids.size(), "duplicate in " + ids);
        }
    }

    @Test
    void everyPoolEntryIsHitAcrossManySeeds() throws IOException {
        LibraryData d = lib();
        Set<String> seen1 = new HashSet<>();
        Set<String> seen2 = new HashSet<>();
        Random rnd = new Random(20261117L);
        for (int seed = 0; seed < 2000; seed++) {
            seen1.addAll(LoadoutRoller.roll(pool(d), d.tier(1).loadout, 0, rnd));
            seen2.addAll(LoadoutRoller.roll(pool(d), d.tier(2).loadout, 0, rnd));
        }
        for (FocusDef f : d.foci.values()) {
            if (f.minTier <= 1 && !"none".equals(f.slot)) {
                assertTrue(seen1.contains(f.id), f.id + " never rolled on tier 1");
            }
            if (!"none".equals(f.slot)) {
                assertTrue(seen2.contains(f.id), f.id + " never rolled on tier 2");
            }
        }
        assertFalse(seen1.contains("healing_aura"), "heal_ally has no slot and is not rolled");
        assertFalse(seen2.contains("healing_aura"), "heal_ally has no slot and is not rolled");
    }

    @Test
    void budgetIsNeverExceeded() throws IOException {
        LibraryData d = lib();
        for (int tier = 1; tier <= 2; tier++) {
            LoadoutRoller.Config cfg = d.tier(tier).loadout;
            for (int seed = 0; seed < 3000; seed++) {
                List<String> ids = LoadoutRoller.roll(pool(d), cfg, seed % 3 == 0 ? -1 : 0, new Random(seed));
                int sum = LoadoutRoller.rolledComplexity(ids, pool(d), cfg);
                assertTrue(sum <= cfg.complexityBudget, "tier " + tier + " seed " + seed + " spent " + sum + " " + ids);
            }
        }
    }

    @Test
    void aFastFillerIsAlwaysAmongTheDirectFoci() throws IOException {
        LibraryData d = lib();
        for (int tier = 1; tier <= 2; tier++) {
            LoadoutRoller.Config cfg = d.tier(tier).loadout;
            for (int seed = 0; seed < 3000; seed++) {
                List<String> ids = LoadoutRoller.roll(pool(d), cfg, 0, new Random(seed));
                boolean filler = false;
                for (String id : ids) {
                    FocusDef f = d.focus(id);
                    if (isDirect(f) && f.complexity <= cfg.fastFillerComplexity) {
                        filler = true;
                    }
                }
                assertTrue(filler, "no fast filler on tier " + tier + " seed " + seed + ": " + ids);
            }
        }
    }

    @Test
    void noRoleExceedsTheCapAmongRolledFoci() throws IOException {
        LibraryData d = lib();
        LoadoutRoller.Config cfg = d.tier(2).loadout;
        for (int seed = 0; seed < 2000; seed++) {
            List<String> ids = LoadoutRoller.roll(pool(d), cfg, 0, new Random(seed));
            java.util.Map<String, Integer> roles = new java.util.HashMap<>();
            for (String id : ids) {
                if (!cfg.fixed.contains(id)) {
                    roles.merge(d.focus(id).role, 1, Integer::sum);
                }
            }
            for (java.util.Map.Entry<String, Integer> e : roles.entrySet()) {
                assertTrue(e.getValue() <= cfg.maxPerRole, e + " in " + ids);
            }
        }
    }

    @Test
    void aDimensionNamedFocusIsFavouredInItsDimension() throws IOException {
        LibraryData d = lib();
        LoadoutRoller.Config cfg = d.tier(2).loadout;
        int inNether = 0;
        int inOverworld = 0;
        Random rndNether = new Random(31L);
        Random rndOverworld = new Random(32L);
        for (int seed = 0; seed < 4000; seed++) {
            if (LoadoutRoller.roll(pool(d), cfg, -1, rndNether).contains("nethers_wrath")) {
                inNether++;
            }
            if (LoadoutRoller.roll(pool(d), cfg, 0, rndOverworld).contains("nethers_wrath")) {
                inOverworld++;
            }
        }
        assertTrue(inNether > inOverworld * 1.4D, "nether " + inNether + " vs overworld " + inOverworld);
    }

    @Test
    void aTierOneMageInTheNetherStillNeverRollsNethersWrath() throws IOException {
        LibraryData d = lib();
        for (int seed = 0; seed < 1000; seed++) {
            assertFalse(LoadoutRoller.roll(pool(d), d.tier(1).loadout, -1, new Random(seed)).contains("nethers_wrath"));
        }
    }

    @Test
    void complexityBiasShiftsTowardHeavyFociAsTheTierRises() throws IOException {
        LibraryData d = lib();
        LoadoutRoller.Config flat = d.tier(2).loadout;
        LoadoutRoller.Config heavy = new LoadoutRoller.Config();
        heavy.tier = 2;
        heavy.fixed = new ArrayList<>(flat.fixed);
        heavy.slots = new ArrayList<>(flat.slots);
        heavy.wildcards = flat.wildcards;
        heavy.complexityBudget = 1000;
        heavy.complexityBias = 2.0D;
        LoadoutRoller.Config light = new LoadoutRoller.Config();
        light.tier = 2;
        light.fixed = new ArrayList<>(flat.fixed);
        light.slots = new ArrayList<>(flat.slots);
        light.wildcards = flat.wildcards;
        light.complexityBudget = 1000;
        light.complexityBias = -2.0D;
        long heavySum = 0;
        long lightSum = 0;
        for (int seed = 0; seed < 1500; seed++) {
            heavySum += LoadoutRoller.rolledComplexity(LoadoutRoller.roll(pool(d), heavy, 0, new Random(seed)), pool(d), heavy);
            lightSum += LoadoutRoller.rolledComplexity(LoadoutRoller.roll(pool(d), light, 0, new Random(seed)), pool(d), light);
        }
        assertTrue(heavySum > lightSum, "heavy " + heavySum + " light " + lightSum);
    }

    @Test
    void sameSeedGivesTheSameLoadout() throws IOException {
        LibraryData d = lib();
        assertEquals(LoadoutRoller.roll(pool(d), d.tier(2).loadout, 0, new Random(42)),
                LoadoutRoller.roll(pool(d), d.tier(2).loadout, 0, new Random(42)));
    }

    @Test
    void missingFixedFociAreSkippedNotFatal() throws IOException {
        LibraryData d = lib();
        List<FocusDef> shrunk = new ArrayList<>(d.foci.values());
        shrunk.removeIf(f -> f.id.equals("rift"));
        List<String> ids = LoadoutRoller.roll(shrunk, d.tier(1).loadout, 0, new Random(1));
        assertFalse(ids.contains("rift"));
        assertEquals(7, ids.size());
    }
}
