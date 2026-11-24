package com.apocollis.aqtweaks.thaumcraft.battlemage;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudGuardTest {

    private static CloudGuard.Params params() {
        CloudGuard.Params p = new CloudGuard.Params();
        p.range = 3.5D;
        p.nearRange = 6.0D;
        p.meleeReach = 2.5D;
        p.touchReach = 3.0D;
        p.meleeMin = 1;
        p.meleeMax = 2;
        p.cooldownTicks = 30;
        p.closeTicks = 20;
        p.maxTicks = 100;
        return p;
    }

    private static CloudGuard.Input in(long now, double toCloud, double toMage) {
        CloudGuard.Input i = new CloudGuard.Input();
        i.now = now;
        i.armed = true;
        i.playerPresent = true;
        i.playerToCloud = toCloud;
        i.playerToMage = toMage;
        i.knockBackReady = true;
        return i;
    }

    @Test
    void doesNothingWhenNotArmedOrNoPlayerOrOutOfRange() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        CloudGuard.Input i = in(0, 2.0D, 2.0D);
        i.armed = false;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), i, r));
        i = in(1, 2.0D, 2.0D);
        i.playerPresent = false;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), i, r));
        assertEquals(CloudGuard.Action.NONE, g.step(params(), in(2, 3.6D, 2.0D), r)); // too far from the cloud
        assertEquals(CloudGuard.Action.NONE, g.step(params(), in(3, 2.0D, 6.1D), r)); // mage not near
        assertFalse(g.active());
    }

    @Test
    void meleeThenKnockBackWhenThePlayerIsInMeleeReach() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        assertEquals(CloudGuard.Action.MELEE, g.step(params(), in(0, 2.0D, 2.0D), r));
        assertTrue(g.burstSize() >= 1 && g.burstSize() <= 2);
        CloudGuard.Input burst = in(1, 2.0D, 2.0D);
        burst.burstActive = true;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), burst, r));
        assertEquals(CloudGuard.Action.KNOCK_BACK, g.step(params(), in(20, 2.0D, 2.0D), r));
        assertFalse(g.active());
    }

    @Test
    void knockBackAloneBetweenMeleeAndTouchReach() {
        CloudGuard g = new CloudGuard();
        assertEquals(CloudGuard.Action.KNOCK_BACK, g.step(params(), in(0, 3.0D, 2.8D), new Random(1L)));
        assertFalse(g.active());
    }

    @Test
    void aPlayerSteppingTowardTheCloudInReachSkipsTheMelee() {
        CloudGuard g = new CloudGuard();
        CloudGuard.Input i = in(0, 2.0D, 2.0D);
        i.approaching = true;
        assertEquals(CloudGuard.Action.KNOCK_BACK, g.step(params(), i, new Random(1L)));
    }

    @Test
    void walksInWhenBeyondTouchReachThenKnocksBackOnceInReach() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        assertEquals(CloudGuard.Action.WALK, g.step(params(), in(0, 3.0D, 5.0D), r));
        assertEquals(CloudGuard.Action.WALK, g.step(params(), in(5, 3.0D, 4.0D), r));
        assertEquals(CloudGuard.Action.KNOCK_BACK, g.step(params(), in(8, 3.0D, 2.9D), r));
        assertFalse(g.active());
    }

    @Test
    void walkingGivesUpAfterTheCloseTicksAndTheCooldownApplies() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        assertEquals(CloudGuard.Action.WALK, g.step(params(), in(0, 3.0D, 5.5D), r));
        assertEquals(CloudGuard.Action.NONE, g.step(params(), in(20, 3.0D, 5.5D), r));
        assertFalse(g.active());
        // inside the 30 tick cooldown nothing starts, after it the guard can start again
        assertEquals(CloudGuard.Action.NONE, g.step(params(), in(30, 2.0D, 2.0D), r));
        assertEquals(CloudGuard.Action.MELEE, g.step(params(), in(50, 2.0D, 2.0D), r));
    }

    @Test
    void knockBackOnCooldownMeansMeleeOnly() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        CloudGuard.Input i = in(0, 2.0D, 2.0D);
        i.knockBackReady = false;
        assertEquals(CloudGuard.Action.MELEE, g.step(params(), i, r));
        CloudGuard.Input after = in(20, 2.0D, 2.0D);
        after.knockBackReady = false;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), after, r));
        assertFalse(g.active());
    }

    @Test
    void knockBackNotReadyOutsideMeleeReachWalksIntoMeleeReach() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        CloudGuard.Input i = in(0, 3.0D, 2.8D);
        i.knockBackReady = false;
        assertEquals(CloudGuard.Action.WALK, g.step(params(), i, r));
        CloudGuard.Input close = in(4, 3.0D, 2.4D);
        close.knockBackReady = false;
        assertEquals(CloudGuard.Action.MELEE, g.step(params(), close, r));
    }

    @Test
    void theSequenceEndsWhenTheAuraRunsOutOrThePlayerLeaves() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        g.step(params(), in(0, 3.0D, 5.0D), r);
        assertTrue(g.active());
        CloudGuard.Input gone = in(5, 3.0D, 5.0D);
        gone.armed = false;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), gone, r));
        assertFalse(g.active());
        // cooldown after the end: 5 + 30
        assertEquals(CloudGuard.Action.NONE, g.step(params(), in(20, 2.0D, 2.0D), r));
        g.step(params(), in(40, 3.0D, 5.0D), r);
        CloudGuard.Input left = in(42, 3.0D, 5.0D);
        left.playerPresent = false;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), left, r));
        assertFalse(g.active());
    }

    @Test
    void stunEndsTheSequenceWithoutACooldown() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        g.step(params(), in(0, 3.0D, 5.0D), r);
        CloudGuard.Input stun = in(1, 3.0D, 5.0D);
        stun.stunned = true;
        assertEquals(CloudGuard.Action.NONE, g.step(params(), stun, r));
        assertFalse(g.active());
        assertEquals(CloudGuard.Action.MELEE, g.step(params(), in(2, 2.0D, 2.0D), r));
    }

    @Test
    void aSequenceThatRunsPastItsSafetyLimitIsDropped() {
        CloudGuard g = new CloudGuard();
        Random r = new Random(1L);
        CloudGuard.Params p = params();
        g.step(p, in(0, 2.0D, 2.0D), r); // MELEE
        CloudGuard.Input burst = in(50, 2.0D, 2.0D);
        burst.burstActive = true;
        assertEquals(CloudGuard.Action.NONE, g.step(p, burst, r));
        assertTrue(g.active());
        burst.now = 100;
        assertEquals(CloudGuard.Action.NONE, g.step(p, burst, r));
        assertFalse(g.active());
    }
}
