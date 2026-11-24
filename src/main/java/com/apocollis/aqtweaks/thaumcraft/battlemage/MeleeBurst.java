package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.Random;

/**
 * Timing of the melee burst (plan 3.2, rule 10): 2 to 3 weapon attacks, then out, then a cooldown before the next
 * burst. Pure state, unit tested; the AI feeds it the entity's tick count.
 */
public final class MeleeBurst {

    private boolean active;
    private int planned;
    private int done;
    private long startedAt;
    private long nextAttackAt;
    private long cooldownUntil;

    public boolean active() {
        return active;
    }

    public int done() {
        return done;
    }

    public boolean canStart(long now) {
        return !active && now >= cooldownUntil;
    }

    /** Number of attacks for one burst, {@code min} to {@code max} inclusive. */
    public static int plan(int min, int max, Random rnd) {
        int lo = Math.max(1, min);
        int hi = Math.max(lo, max);
        return lo + rnd.nextInt(hi - lo + 1);
    }

    public void start(long now, int plannedAttacks) {
        active = true;
        planned = Math.max(1, plannedAttacks);
        done = 0;
        startedAt = now;
        nextAttackAt = now;
    }

    public boolean attackDue(long now) {
        return active && now >= nextAttackAt;
    }

    public void onAttack(long now, int intervalTicks) {
        done++;
        nextAttackAt = now + Math.max(1, intervalTicks);
    }

    /** All attacks made, or the burst ran past its time limit (the target stepped away, the path is blocked). */
    public boolean finished(long now, int maxTicks) {
        return active && (done >= planned || now - startedAt >= maxTicks);
    }

    public void end(long now, int cooldownTicks) {
        active = false;
        cooldownUntil = now + Math.max(0, cooldownTicks);
    }

    /** Drops the burst without a cooldown (Stun, target gone). */
    public void abort() {
        active = false;
    }
}
