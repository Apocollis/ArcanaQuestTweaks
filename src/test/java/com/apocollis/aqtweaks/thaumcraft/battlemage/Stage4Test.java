package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage4Test {

    @Test
    void phasesRiseAndNeverFall() {
        assertEquals(0, BattlemagePhase.next(0, 0.9D, 0.66D, 0.33D));
        assertEquals(1, BattlemagePhase.next(0, 0.6D, 0.66D, 0.33D));
        assertEquals(2, nextSkip());
        assertEquals(2, BattlemagePhase.next(2, 0.9D, 0.66D, 0.33D));
        assertEquals(1, BattlemagePhase.next(1, 0.5D, 0.66D, 0.33D));
    }

    private static int nextSkip() {
        return BattlemagePhase.next(0, 0.2D, 0.66D, 0.33D);
    }

    @Test
    void phaseNumbersPickTheRightTier() {
        assertEquals(1.0D, BattlemagePhase.cooldownScale(0, 0.75D, 0.6D));
        assertEquals(0.75D, BattlemagePhase.cooldownScale(1, 0.75D, 0.6D));
        assertEquals(0.6D, BattlemagePhase.cooldownScale(2, 0.75D, 0.6D));
        assertEquals(1.5D, BattlemagePhase.regenScale(2, 1.25D, 1.5D));
        assertEquals(0, BattlemagePhase.extraBats(0, 1, 1));
        assertEquals(1, BattlemagePhase.extraBats(1, 1, 1));
        assertEquals(100, BattlemagePhase.blinkCooldown(1, 100, 60));
        assertEquals(60, BattlemagePhase.blinkCooldown(2, 100, 60));
        assertEquals(200, BattlemagePhase.healRegenTicks(1, 200, 0.75D));
        assertEquals(150, BattlemagePhase.healRegenTicks(2, 200, 0.75D));
    }

    @Test
    void lootingRaisesChancesAndCountsButNeverAboveOne() {
        assertEquals(0.44D + 0.15D, DropRules.lootedChance(0.44D, 3, 0.05D), 1e-9);
        assertEquals(1.0D, DropRules.lootedChance(0.98D, 3, 0.05D), 1e-9);
        Random rnd = new Random(7);
        for (int i = 0; i < 200; i++) {
            int c = DropRules.count(2, 4, 3, rnd);
            assertTrue(c >= 2 && c <= 7, "count " + c);
        }
    }

    @Test
    void oneGearPieceByDefaultAndAtMostTwoWithLooting() {
        Random rnd = new Random(3);
        for (int i = 0; i < 100; i++) {
            assertEquals(1, DropRules.pieces(0.15D, 0, 2, rnd));
            int n = DropRules.pieces(0.15D, 3, 2, rnd);
            assertTrue(n == 1 || n == 2);
        }
        int extras = 0;
        for (int i = 0; i < 4000; i++) {
            if (DropRules.pieces(0.15D, 3, 2, rnd) == 2) extras++;
        }
        assertEquals(0.45D, extras / 4000.0D, 0.05D);
    }

    @Test
    void distinctPicksFollowTheWeightsAndNeverRepeat() {
        Random rnd = new Random(11);
        double[] w = {1, 3, 5, 0};
        int[] firsts = new int[4];
        for (int i = 0; i < 6000; i++) {
            List<Integer> pick = DropRules.pickDistinct(w, 2, rnd);
            assertEquals(2, pick.size());
            assertEquals(2, new HashSet<>(pick).size());
            assertFalse(pick.contains(3));
            firsts[pick.get(0)]++;
        }
        assertTrue(firsts[2] > firsts[1] && firsts[1] > firsts[0]);
        assertEquals(5.0D / 9.0D, firsts[2] / 6000.0D, 0.04D);
    }

    @Test
    void fewerCandidatesThanWantedGivesWhatThereIs() {
        assertEquals(1, DropRules.pickDistinct(new double[] {2, 0, 0}, 2, new Random(1)).size());
        assertTrue(DropRules.pickDistinct(new double[0], 2, new Random(1)).isEmpty());
    }

    @Test
    void gearWeightsMatchThePlan() {
        assertEquals(1.0D, DropRules.defaultGearWeight("mainhand"));
        assertEquals(1.0D, DropRules.defaultGearWeight("chest"));
        for (String slot : new String[] {"head", "legs", "feet", "offhand"}) {
            assertEquals(2.0D, DropRules.defaultGearWeight(slot));
        }
    }

    @Test
    void focusWeightsAreFixedRolledHighOrTheOverride() {
        assertEquals(1.0D, DropRules.focusWeight(null, true, 1, 1, 3, 5));
        assertEquals(3.0D, DropRules.focusWeight(null, false, 1, 1, 3, 5));
        assertEquals(5.0D, DropRules.focusWeight(null, false, 2, 1, 3, 5));
        assertEquals(9.0D, DropRules.focusWeight(9.0D, false, 2, 1, 3, 5));
    }

    @Test
    void theBundledLibraryCarriesTheDropTables() throws Exception {
        LibraryData d = LibraryParser.parse(LibraryParserTest.bundledText());
        assertEquals(8, d.tier(1).drops.size());
        assertEquals(8, d.tier(2).drops.size());
        assertEquals(2, d.tier(1).baubleDrops.size());
        assertEquals(3, d.tier(2).baubleDrops.size());
        DropEntry crate = d.tier(1).drops.get(6);
        assertEquals("lootchests:loot_crate", crate.item);
        assertEquals(3, crate.meta);
        assertEquals(0.44D, d.tier(1).drops.get(3).chance, 1e-9);
        assertEquals(0.10D, d.ai.surgeLowHealthFraction, 1e-9);
        assertEquals(0.66D, d.ai.phaseEmpoweredBelow, 1e-9);
        assertEquals(600, d.tier(1).ai.resetAfterTicks);
        assertEquals(0.6D, d.tier(1).ai.meleeCloseInChance, 1e-9);
        assertEquals(14.0D, d.tier(1).ai.meleeCloseInMaxRange, 1e-9);
        assertEquals(0.5D, d.tier(1).ai.meleeStrikeBlinkChance, 1e-9);
        assertEquals(4, d.tier(1).ai.healBudgetMax);
        assertEquals(100, d.tier(1).ai.healBudgetRegenTicks);
        assertEquals(5, d.aiFor(1, "swarm_mage").healAllyBudgetMax);
        assertEquals(100, d.aiFor(1, "swarm_mage").healAllyBudgetRegenTicks);
        assertEquals(0.65D, d.tier(1).ai.healHpFraction, 1e-9);
        assertEquals(0.30D, d.tier(1).ai.awayHpFraction, 1e-9);
        assertEquals(80, d.tier(1).ai.peelCooldownTicks);
        assertEquals(30, d.tier(1).ai.sidestepCooldownTicks);
        assertEquals(80, d.tier(1).ai.blinkCooldownTicks);
        assertEquals(24.0D, d.tier(1).ai.meleeStrikeBlinkMaxRange, 1e-9);
        assertEquals(10, d.tier(1).ai.meleeStrikeInWindowTicks);
        assertEquals(10, d.tier(1).ai.meleeStrikeOutWindowTicks);
        assertEquals(200, d.tier(1).ai.repositionBlinkIntervalTicks);
        assertEquals(0.4D, d.tier(1).ai.repositionBlinkChance, 1e-9);
        Set<String> ids = new HashSet<>();
        for (DropEntry e : d.tier(2).baubleDrops) ids.add(e.item);
        assertTrue(ids.contains("crimsonrevelations:runic_girdle"));
    }
}
