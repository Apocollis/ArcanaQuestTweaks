package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.Random;

/**
 * The Swarm Mage's guard for its own Healing Aura (plan 20). While the aura is out (or was cast moments ago) and a hostile
 * player closes on the cloud with the mage near, the mage drives the player off: melee strikes then Knock Back, with Knock
 * Back alone when the player is stepping toward the cloud and already in reach. Pure and unit tested; the entity feeds it the
 * situation every AI tick and carries out the action it returns.
 *
 * <pre>
 *   trigger: guard armed (cloud live, or inside cloudGuardCastTicks of the cast), no cooldown, a player within
 *            cloudGuardRange of the cloud centre AND within cloudGuardNearRange of the mage
 *     stepping toward the cloud, in touch reach, Knock Back ready : KNOCK_BACK at once
 *     within melee reach                                          : MELEE (1 to 2 strikes) -> KNOCK_BACK if ready and in reach
 *     melee..touch reach, Knock Back ready                        : KNOCK_BACK
 *     beyond touch reach (or Knock Back not ready)                : WALK toward the player for at most closeTicks, then
 *                                                                   KNOCK_BACK once in reach, or MELEE if it is only melee reach
 * </pre>
 *
 * Every sequence ends with {@code cooldownTicks} before the next one. Stun, the player leaving, or the aura running out
 * ends it at once (the cooldown still applies after a normal end, not after a Stun).
 */
public final class CloudGuard {

    public enum State { IDLE, APPROACH, MELEE }

    public enum Action {
        /** Nothing to do (idle, cooling down, or a melee burst is running). */
        NONE,
        /** Start a melee burst of {@link CloudGuard#burstSize()} strikes. */
        MELEE,
        /** Cast Knock Back at the player. */
        KNOCK_BACK,
        /** Walk toward the player. */
        WALK
    }

    /** The tuning of one sequence. */
    public static final class Params {
        public double range = 3.5D;
        public double nearRange = 6.0D;
        public double meleeReach = 2.5D;
        public double touchReach = 3.0D;
        public int meleeMin = 1;
        public int meleeMax = 2;
        public int cooldownTicks = 30;
        public int closeTicks = 20;
        /** Safety limit for a whole sequence, walking and the melee burst included. */
        public int maxTicks = 100;
    }

    /** The situation this tick. */
    public static final class Input {
        public long now;
        /** The aura cloud is alive, or the mage cast it within {@code cloudGuardCastTicks}. */
        public boolean armed;
        public boolean playerPresent;
        /** Player distance to the cloud centre. */
        public double playerToCloud;
        /** Player distance to the mage. */
        public double playerToMage;
        /** The player is closer to the cloud centre than a moment ago. */
        public boolean approaching;
        public boolean knockBackReady;
        /** The melee burst this sequence started is still running. */
        public boolean burstActive;
        public boolean stunned;
    }

    private State state = State.IDLE;
    private long enteredAt;
    private long cooldownUntil;
    private int burstSize = 1;

    public State state() {
        return state;
    }

    public boolean active() {
        return state != State.IDLE;
    }

    /** Strikes the current melee burst should make (valid after {@link Action#MELEE} was returned). */
    public int burstSize() {
        return burstSize;
    }

    /** Drops the sequence with no cooldown (Stun, a new fight, a reload). */
    public void reset() {
        state = State.IDLE;
    }

    public Action step(Params p, Input in, Random rnd) {
        if (in.stunned) {
            state = State.IDLE;
            return Action.NONE;
        }
        if (state != State.IDLE) {
            if (!in.armed || !in.playerPresent || in.now - enteredAt >= p.maxTicks) {
                finish(p, in.now);
                return Action.NONE;
            }
        }
        switch (state) {
            case IDLE:
                return idle(p, in, rnd);
            case APPROACH:
                return approach(p, in, rnd);
            case MELEE:
            default:
                return melee(p, in);
        }
    }

    private boolean triggered(Params p, Input in) {
        return in.armed && in.playerPresent && in.playerToCloud <= p.range && in.playerToMage <= p.nearRange;
    }

    private Action idle(Params p, Input in, Random rnd) {
        if (in.now < cooldownUntil || !triggered(p, in)) {
            return Action.NONE;
        }
        enteredAt = in.now;
        boolean inTouch = in.playerToMage <= p.touchReach;
        if (in.approaching && inTouch && in.knockBackReady) {
            return knockBack(p, in.now);
        }
        if (in.playerToMage <= p.meleeReach) {
            return startMelee(p, in, rnd);
        }
        if (inTouch && in.knockBackReady) {
            return knockBack(p, in.now);
        }
        state = State.APPROACH;
        return Action.WALK;
    }

    private Action approach(Params p, Input in, Random rnd) {
        if (in.now - enteredAt >= p.closeTicks) {
            finish(p, in.now);
            return Action.NONE;
        }
        if (in.playerToMage <= p.touchReach && in.knockBackReady) {
            return knockBack(p, in.now);
        }
        if (in.playerToMage <= p.meleeReach) {
            return startMelee(p, in, rnd);
        }
        return Action.WALK;
    }

    private Action melee(Params p, Input in) {
        if (in.burstActive) {
            return Action.NONE;
        }
        if (in.knockBackReady && in.playerToMage <= p.touchReach) {
            return knockBack(p, in.now);
        }
        finish(p, in.now);
        return Action.NONE;
    }

    private Action startMelee(Params p, Input in, Random rnd) {
        state = State.MELEE;
        burstSize = MeleeBurst.plan(p.meleeMin, p.meleeMax, rnd);
        return Action.MELEE;
    }

    private Action knockBack(Params p, long now) {
        finish(p, now);
        return Action.KNOCK_BACK;
    }

    private void finish(Params p, long now) {
        state = State.IDLE;
        cooldownUntil = now + Math.max(0, p.cooldownTicks);
    }
}
