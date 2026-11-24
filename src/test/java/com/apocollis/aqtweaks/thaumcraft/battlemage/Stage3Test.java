package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The pure rules of Stage 3 (plan 22): shield, heal budget, defence decisions, dodge, target score, reachability. */
class Stage3Test {

    // ---- ShieldModel ----

    @Test
    void theShieldGainsOnePointPerIntervalUpToTheCap() {
        ShieldModel m = new ShieldModel(40, 40, 0.5D);
        double v = 10.0D;
        v = m.next(0L, v, 16.0D, true);
        assertEquals(11.0D, v, "the first point is due at once (nothing was waiting)");
        assertEquals(11.0D, m.next(39L, v, 16.0D, true));
        assertEquals(12.0D, m.next(40L, 11.0D, 16.0D, true));
        double full = 16.0D;
        assertEquals(16.0D, m.next(1000L, full, 16.0D, true));
    }

    @Test
    void aBrokenShieldWaitsBeforeTheFirstPointAndWidensTheBandUntilHalf() {
        ShieldModel m = new ShieldModel(40, 40, 0.5D);
        assertEquals(0.0D, m.next(100L, 0.0D, 16.0D, true));
        assertTrue(m.isBroken(0.0D, 16.0D));
        assertTrue(m.widenBand(0.0D, 16.0D));
        assertEquals(0.0D, m.next(139L, 0.0D, 16.0D, true), "still inside the 40 tick wait");
        double v = m.next(140L, 0.0D, 16.0D, true);
        assertEquals(1.0D, v);
        assertTrue(m.widenBand(v, 16.0D), "still under half");
        long t = 140L;
        while (v < 8.0D) {
            t += 40L;
            v = m.next(t, v, 16.0D, true);
        }
        assertFalse(m.widenBand(v, 16.0D), "back to half: the band returns");
    }

    @Test
    void theShieldNeverLowersAndNoCapMeansNoShield() {
        ShieldModel m = new ShieldModel(40, 40, 0.5D);
        assertEquals(30.0D, m.next(5L, 30.0D, 16.0D, true), "above the cap is left alone");
        assertEquals(3.0D, m.next(5L, 3.0D, 0.0D, true));
        assertFalse(m.widenBand(0.0D, 0.0D));
    }

    @Test
    void aPointIsNotAddedWhenItCannotBePaid() {
        ShieldModel m = new ShieldModel(40, 40, 0.5D);
        assertEquals(5.0D, m.next(0L, 5.0D, 16.0D, false));
        assertEquals(6.0D, m.next(40L, 5.0D, 16.0D, true));
    }

    // ---- HealBudget ----

    @Test
    void healChargesSpendAndComeBackOneAtATime() {
        HealBudget h = new HealBudget(3, 200);
        assertEquals(3, h.charges());
        assertTrue(h.spend(0L));
        assertTrue(h.spend(10L));
        assertTrue(h.spend(20L));
        assertFalse(h.canHeal());
        assertFalse(h.spend(30L));
        h.tick(199L);
        assertEquals(0, h.charges());
        h.tick(200L);
        assertEquals(1, h.charges());
        h.tick(399L);
        assertEquals(1, h.charges());
        h.tick(400L);
        assertEquals(2, h.charges());
        h.tick(5000L);
        assertEquals(3, h.charges(), "never above the max");
    }

    // ---- DefenseRules ----

    @Test
    void theStrictEscapeNeedsAllFourConditions() {
        // low health, player close, pressured, Knock Back spent: yes
        assertTrue(DefenseRules.awayEscape(0.2D, 0.25D, 3.0D, 4.0D, 40, 30, false, false, -1L, 100));
        assertFalse(DefenseRules.awayEscape(0.3D, 0.25D, 3.0D, 4.0D, 40, 30, false, false, -1L, 100), "health not low");
        assertFalse(DefenseRules.awayEscape(0.2D, 0.25D, 6.0D, 4.0D, 40, 30, false, false, -1L, 100), "player not close");
        assertFalse(DefenseRules.awayEscape(0.2D, 0.25D, 3.0D, 4.0D, 5, 30, false, false, -1L, 100), "can still walk away");
        assertTrue(DefenseRules.awayEscape(0.2D, 0.25D, 3.0D, 4.0D, 5, 30, true, false, -1L, 100), "cornered counts as cannot escape");
        assertFalse(DefenseRules.awayEscape(0.2D, 0.25D, 3.0D, 4.0D, 40, 30, false, true, -1L, 100), "Knock Back is ready: use it first");
        assertTrue(DefenseRules.awayEscape(0.2D, 0.25D, 3.0D, 4.0D, 40, 30, false, true, 60L, 100), "Knock Back just used and the player is still close");
        assertFalse(DefenseRules.awayEscape(0.2D, 0.25D, 3.0D, 4.0D, 40, 30, false, true, 150L, 100), "Knock Back used long ago and ready again");
    }

    @Test
    void healingRetreatAndPeelRules() {
        assertTrue(DefenseRules.healInPlace(0.5D, 0.6D, true, true, 9.0D, 4.0D));
        assertFalse(DefenseRules.healInPlace(0.5D, 0.6D, false, true, 9.0D, 4.0D), "no charge");
        assertFalse(DefenseRules.healInPlace(0.5D, 0.6D, true, true, 3.0D, 4.0D), "melee pressure: peel first");
        assertFalse(DefenseRules.healInPlace(0.8D, 0.6D, true, true, 9.0D, 4.0D), "healthy");
        assertTrue(DefenseRules.startRetreat(0.5D, 0.5D, true, true));
        assertFalse(DefenseRules.startRetreat(0.5D, 0.5D, false, true));
        assertFalse(DefenseRules.startRetreat(0.5D, 0.5D, true, false));
        assertTrue(DefenseRules.endRetreat(0.8D, 0.75D, true, 10L, 160));
        assertTrue(DefenseRules.endRetreat(0.4D, 0.75D, false, 10L, 160), "out of charges");
        assertTrue(DefenseRules.endRetreat(0.4D, 0.75D, true, 160L, 160), "out of time");
        assertFalse(DefenseRules.endRetreat(0.4D, 0.75D, true, 10L, 160));
        assertTrue(DefenseRules.peel(2.5D, 3.0D, true, true, false));
        assertFalse(DefenseRules.peel(2.5D, 3.0D, true, true, true), "a melee burst is under way");
        assertFalse(DefenseRules.peel(3.5D, 3.0D, true, true, false));
        assertFalse(DefenseRules.peel(2.5D, 3.0D, false, true, false));
        assertFalse(DefenseRules.peel(2.5D, 3.0D, true, false, false));
    }

    // ---- DodgePlanner ----

    @Test
    void aProjectileOnACollisionCourseIsDodgedToTheFarSide() {
        // projectile at x=-10 moving +x at 1 block/tick, mage at the origin: dead centre hit
        int side = DodgePlanner.sideFor(-10.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.4D, 12);
        assertTrue(side == 1 || side == -1);
        // slightly offset to +z: step to -z side, i.e. away from the line of fire
        int off = DodgePlanner.sideFor(-10.0D, 0.0D, 0.5D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.4D, 12);
        double[] p = DodgePlanner.perpendicular(1.0D, 0.0D, off);
        assertTrue(p[1] < 0.0D, "steps toward -z, away from the projectile's +z offset");
    }

    @Test
    void aProjectileThatMissesOrIsBehindIsIgnored() {
        assertEquals(0, DodgePlanner.sideFor(-10.0D, 0.0D, 5.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.4D, 12), "passes 5 blocks wide");
        assertEquals(0, DodgePlanner.sideFor(10.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.4D, 12), "moving away");
        assertEquals(0, DodgePlanner.sideFor(-40.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.4D, 12), "too far to matter yet");
        assertEquals(0, DodgePlanner.sideFor(-10.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.4D, 12), "not moving");
    }

    // ---- TargetScorer ----

    @Test
    void theLastAttackerAndTheWoundedWinAndTheCurrentTargetIsSticky() {
        List<TargetScorer.Candidate> l = new ArrayList<>();
        TargetScorer.Candidate near = new TargetScorer.Candidate(1);
        near.distance = 10.0D;
        near.current = true;
        TargetScorer.Candidate hitter = new TargetScorer.Candidate(2);
        hitter.distance = 12.0D;
        hitter.hitMeLast = true;
        l.add(near);
        l.add(hitter);
        assertEquals(2, TargetScorer.pick(l), "the one that hit it last beats the sticky current target");
        hitter.hitMeLast = false;
        assertEquals(1, TargetScorer.pick(l), "otherwise the current target stays");
        TargetScorer.Candidate weak = new TargetScorer.Candidate(3);
        weak.distance = 10.0D;
        weak.healthFraction = 0.1D;
        near.current = false;
        l.add(weak);
        assertEquals(3, TargetScorer.pick(l), "a nearly dead player is worth the switch");
        assertEquals(-1, TargetScorer.pick(new ArrayList<TargetScorer.Candidate>()));
    }

    // ---- UnstickSearch ----

    @Test
    void unstickTriesTheNearestSpotsFirstAndPrefersUp() {
        List<int[]> o = UnstickSearch.offsets(4, 3, 3);
        int[] first = o.get(0);
        assertEquals(1, Math.abs(first[0]) + Math.abs(first[1]) + Math.abs(first[2]));
        int upIndex = -1;
        int downIndex = -1;
        for (int i = 0; i < o.size(); i++) {
            int[] c = o.get(i);
            if (c[0] == 0 && c[2] == 0 && c[1] == 1) {
                upIndex = i;
            }
            if (c[0] == 0 && c[2] == 0 && c[1] == -1) {
                downIndex = i;
            }
        }
        assertTrue(upIndex < downIndex, "one up is tried before one down");
        for (int[] c : o) {
            assertTrue(c[0] * c[0] + c[2] * c[2] <= 16, "stays inside the radius");
            assertTrue(c[1] >= -3 && c[1] <= 3);
        }
    }

    // ---- ReachabilityRule ----

    @Test
    void reachabilityRule() {
        assertTrue(ReachabilityRule.blocked(false, 0, 0.0D, 10.0D, 2.5D), "no path");
        assertTrue(ReachabilityRule.blocked(true, 10, 6.0D, 10.0D, 2.5D), "the path stops 6 blocks short");
        assertFalse(ReachabilityRule.blocked(true, 14, 1.0D, 10.0D, 2.5D));
        assertTrue(ReachabilityRule.blocked(true, 40, 1.0D, 10.0D, 2.5D), "a 40 node detour for 10 blocks");
        assertFalse(ReachabilityRule.blocked(true, 15, 1.0D, 3.0D, 2.5D), "short paths are never too long (minimum 16 nodes)");
    }

    // ---- OpponentReading ----

    @Test
    void aRaisedShieldFavoursAreaOverShots() {
        assertEquals(1.0D, OpponentReading.roleScale("bolt", false));
        assertEquals(OpponentReading.SHOT_SCALE, OpponentReading.roleScale("bolt", true));
        assertEquals(OpponentReading.SHOT_SCALE, OpponentReading.roleScale("ranged_primary", true));
        assertEquals(OpponentReading.AREA_SCALE, OpponentReading.roleScale("aoe", true));
        assertEquals(OpponentReading.AREA_SCALE, OpponentReading.roleScale("debuff", true));
        assertEquals(1.0D, OpponentReading.roleScale("peel", true));
    }

    // ---- bundled tuning ----

    @Test
    void theBundledLibraryCarriesTheStage3Blocks() throws IOException {
        String json;
        try (InputStream in = Stage3Test.class.getResourceAsStream("/assets/aqtweaks/battlemage/cultist_battlemages.json")) {
            assertNotNull(in);
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        LibraryData d = LibraryParser.parse(json);
        assertTrue(d.problems.isEmpty(), d.problems.toString());
        AiTuning ai = d.aiFor(1, null);
        assertEquals(40, ai.shieldRechargeIntervalTicks);
        assertEquals(40, ai.shieldBreakWaitTicks);
        assertEquals(4, ai.healBudgetMax);
        assertEquals(100, ai.healBudgetRegenTicks);
        assertEquals(0.30D, ai.awayHpFraction);
        assertEquals(300, ai.riftCooldownMinTicks);
        assertEquals(400, ai.riftCooldownMaxTicks);
        assertEquals(12, ai.riftMaxThickness);
        assertEquals(24, ai.riftMaxHolesPerCast);
        assertEquals(3, ai.suffocationTicks);
        assertEquals(16.0D, d.tier(1).shieldMax);
        assertEquals(28.0D, d.tier(2).shieldMax);
        assertNull(d.variant("nope"));
    }
}
