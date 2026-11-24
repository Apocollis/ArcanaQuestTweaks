package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllyHealPlannerTest {

    private static AllyHealPlanner.Params params() {
        AllyHealPlanner.Params p = new AllyHealPlanner.Params();
        p.hpFraction = 0.6D;
        p.range = 16.0D;
        p.castDistance = 2.5D;
        return p;
    }

    private static AllyHealPlanner.State ready() {
        AllyHealPlanner.State s = new AllyHealPlanner.State();
        s.now = 100;
        s.healReadyAt = 0;
        s.auraReady = true;
        s.blinkReady = true;
        return s;
    }

    private static List<AllyHealPlanner.Ally> allies(double... hpAndDist) {
        List<AllyHealPlanner.Ally> out = new ArrayList<>();
        for (int i = 0; i + 1 < hpAndDist.length; i += 2) {
            out.add(new AllyHealPlanner.Ally(i / 2, hpAndDist[i] * 20.0D, 20.0D, hpAndDist[i + 1]));
        }
        return out;
    }

    @Test
    void nobodyHurtMeansNothing() {
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(1.0, 3.0, 0.7, 4.0), params(), ready()).action);
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(new ArrayList<AllyHealPlanner.Ally>(), params(), ready()).action);
    }

    @Test
    void theThresholdIsStrictlyBelow() {
        // exactly 60% is not "below 60%"
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(0.6, 2.0), params(), ready()).action);
        assertEquals(AllyHealPlanner.Action.CAST, AllyHealPlanner.plan(allies(0.59, 2.0), params(), ready()).action);
    }

    @Test
    void theLowestFractionInRangeIsServedFirst() {
        AllyHealPlanner.Decision d = AllyHealPlanner.plan(allies(0.5, 3.0, 0.2, 5.0, 0.4, 2.0), params(), ready());
        assertEquals(1, d.allyIndex);
        assertEquals(AllyHealPlanner.Action.BLINK, d.action, "5 blocks away: blink next to it");
    }

    @Test
    void anAllyBeyondTheRangeIsIgnoredEvenWhenAlmostDead() {
        AllyHealPlanner.Decision d = AllyHealPlanner.plan(allies(0.05, 17.0, 0.5, 8.0), params(), ready());
        assertEquals(1, d.allyIndex, "the 16 block range drops the first ally");
    }

    @Test
    void aTieOnFractionGoesToTheNearerAlly() {
        AllyHealPlanner.Decision d = AllyHealPlanner.plan(allies(0.3, 9.0, 0.3, 4.0), params(), ready());
        assertEquals(1, d.allyIndex);
    }

    @Test
    void closeEnoughCasts() {
        AllyHealPlanner.Decision d = AllyHealPlanner.plan(allies(0.3, 2.5), params(), ready());
        assertEquals(AllyHealPlanner.Action.CAST, d.action);
        assertEquals(0, d.allyIndex);
    }

    @Test
    void theHealCooldownAndTheFocusCooldownHoldTheCast() {
        AllyHealPlanner.State s = ready();
        s.healReadyAt = 101;
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(0.3, 1.0), params(), s).action);
        s.healReadyAt = 100;
        assertEquals(AllyHealPlanner.Action.CAST, AllyHealPlanner.plan(allies(0.3, 1.0), params(), s).action);
        s.auraReady = false;
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(0.3, 1.0), params(), s).action);
    }

    @Test
    void aFarAllyBlinksWhenBlinkIsReady() {
        assertEquals(AllyHealPlanner.Action.BLINK, AllyHealPlanner.plan(allies(0.3, 10.0), params(), ready()).action);
    }

    @Test
    void withBlinkOnCooldownItWalksUnlessAPlayerIsInMeleeRange() {
        AllyHealPlanner.State s = ready();
        s.blinkReady = false;
        assertEquals(AllyHealPlanner.Action.WALK, AllyHealPlanner.plan(allies(0.3, 10.0), params(), s).action);
        s.playerInMeleeRange = true;
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(0.3, 10.0), params(), s).action);
    }

    @Test
    void blinkingToAnAllyDoesNotWaitForTheHealCooldown() {
        AllyHealPlanner.State s = ready();
        s.healReadyAt = 500;
        assertEquals(AllyHealPlanner.Action.BLINK, AllyHealPlanner.plan(allies(0.3, 10.0), params(), s).action);
    }

    @Test
    void stunStopsEverything() {
        AllyHealPlanner.State s = ready();
        s.stunned = true;
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(0.3, 1.0), params(), s).action);
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(allies(0.3, 10.0), params(), s).action);
    }

    @Test
    void deadOrZeroMaxHealthAlliesAreSkipped() {
        List<AllyHealPlanner.Ally> a = new ArrayList<>();
        a.add(new AllyHealPlanner.Ally(0, 0.0D, 20.0D, 2.0D));
        a.add(new AllyHealPlanner.Ally(1, 5.0D, 0.0D, 2.0D));
        assertEquals(AllyHealPlanner.Action.NONE, AllyHealPlanner.plan(a, params(), ready()).action);
    }
}
