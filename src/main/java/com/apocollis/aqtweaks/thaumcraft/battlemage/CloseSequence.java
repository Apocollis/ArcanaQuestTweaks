package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.Random;

/**
 * The Bombardier Mage's answer to a target that is too close for its area foci (plan 19.8). A small state machine, pure and
 * unit tested; the entity feeds it the situation every AI tick and carries out the action it returns.
 *
 * <pre>
 *   target inside closeRange (the area foci's minimum range):
 *     within meleeRange  : MELEE (1 to 2 weapon attacks) -> KNOCK_BACK (if ready and in touch reach) -> BLINK
 *     meleeRange..closeRange (out of both reaches): straight to BLINK
 *   BLINK needs the shared blink to be ready; otherwise the mage RETREATs on foot and retries each tick.
 * </pre>
 *
 * The sequence is over when the mage blinks, when the target is no longer inside {@code closeRange}, or after
 * {@code maxTicks} (it then starts over, so it can never wedge).
 */
public final class CloseSequence {

    public enum State { IDLE, MELEE, KNOCK_BACK, BLINK }

    public enum Action {
        /** Nothing to do (the volley owns the tick, or a melee burst is running). */
        NONE,
        /** Start a melee burst of {@link CloseSequence#burstSize()} attacks. */
        MELEE,
        /** Cast Knock Back at the target. */
        KNOCK_BACK,
        /** Blink to the volley band. */
        BLINK,
        /** Step back on foot (Blink is on cooldown). */
        RETREAT
    }

    /** The situation this tick. */
    public static final class Input {
        public long now;
        public double dist;
        /** Inside this distance the volley cannot fire (the area foci's minimum range). */
        public double closeRange = 6.0D;
        /** Inside this distance the weapon reaches. */
        public double meleeRange = 3.0D;
        /** Knock Back's touch reach. */
        public double touchReach = 3.0D;
        public boolean knockBackReady;
        public boolean blinkReady;
        /** The melee burst this sequence started is still running. */
        public boolean burstActive;
        public int meleeMin = 1;
        public int meleeMax = 2;
        public int maxTicks = 160;
    }

    private State state = State.IDLE;
    private long enteredAt;
    private int burstSize = 1;

    public State state() {
        return state;
    }

    /** Attacks the current melee burst should make (valid after {@link Action#MELEE} was returned). */
    public int burstSize() {
        return burstSize;
    }

    /** The mage blinked: the sequence is done. */
    public void blinked() {
        state = State.IDLE;
    }

    public void reset() {
        state = State.IDLE;
    }

    /** True while the sequence owns the mage (the volley still keeps its own clock). */
    public boolean active() {
        return state != State.IDLE;
    }

    public Action step(Input in, Random rnd) {
        if (state != State.IDLE && (in.dist >= in.closeRange || in.now - enteredAt >= in.maxTicks)) {
            state = State.IDLE;
            if (in.dist >= in.closeRange) {
                return Action.NONE;
            }
        }
        switch (state) {
            case IDLE:
                if (in.dist >= in.closeRange) {
                    return Action.NONE;
                }
                enteredAt = in.now;
                if (in.dist <= in.meleeRange) {
                    state = State.MELEE;
                    burstSize = MeleeBurst.plan(in.meleeMin, in.meleeMax, rnd);
                    return Action.MELEE;
                }
                state = State.BLINK;
                return blinkOrRetreat(in);
            case MELEE:
                if (in.burstActive) {
                    return Action.NONE;
                }
                state = State.KNOCK_BACK;
                return knockBackOrBlink(in);
            case KNOCK_BACK:
                return knockBackOrBlink(in);
            case BLINK:
            default:
                return blinkOrRetreat(in);
        }
    }

    private Action knockBackOrBlink(Input in) {
        if (in.knockBackReady && in.dist <= in.touchReach) {
            state = State.BLINK;
            return Action.KNOCK_BACK;
        }
        state = State.BLINK;
        return blinkOrRetreat(in);
    }

    private Action blinkOrRetreat(Input in) {
        return in.blinkReady ? Action.BLINK : Action.RETREAT;
    }
}
