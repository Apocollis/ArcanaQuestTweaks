package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The Bombardier Mage's cast rhythm (plan 19.7): a <b>volley</b> fires one area focus every {@code gapTicks}, each of the
 * carried foci once in a random order, then the mage pauses for {@code pauseTicks}. Pure: time is the entity's tick count
 * and "ready" is decided by the caller, so it is unit tested without Minecraft.
 *
 * <ul>
 * <li>No focus fires twice before every focus has fired, and the first of a new volley is never the last of the previous one.
 * <li>A focus the caller reports as not ready (its own cooldown, its previous clouds still alive, out of range) is skipped
 *     for the next ready one and keeps its place in the order.
 * <li>A volley never stalls: {@code maxTicks} after it started it ends where it is and the pause begins.
 * <li>The clock is wall time, so the gap, the pause and the limit keep running while the mage is busy with something else
 *     (the close sequence) and nothing is cast.
 * </ul>
 */
public final class VolleyPlanner {

    private final int gapTicks;
    private final int pauseTicks;
    private final int maxTicks;
    private final Random rnd;

    private final List<String> remaining = new ArrayList<>();
    private boolean inVolley;
    private long volleyStartedAt;
    private long nextShotAt;
    private long pauseUntil;
    private String lastOfPrevious;
    private String lastFired;
    private long lastTouched = Long.MIN_VALUE;

    public VolleyPlanner(int gapTicks, int pauseTicks, int maxTicks, Random rnd) {
        this.gapTicks = Math.max(1, gapTicks);
        this.pauseTicks = Math.max(0, pauseTicks);
        this.maxTicks = Math.max(1, maxTicks);
        this.rnd = rnd;
    }

    /** What the caller can tell the planner about one focus right now. */
    public interface Readiness {
        boolean ready(String focusId);
    }

    /**
     * @param ids every focus of the volley
     * @return the id to cast now, or null when it is not time, nothing is ready, or the mage is pausing
     */
    public String next(long now, Collection<String> ids, Readiness readiness) {
        if (ids.isEmpty()) {
            return null;
        }
        if (lastTouched != Long.MIN_VALUE && now - lastTouched > maxTicks + pauseTicks + gapTicks) {
            reset(); // the mage was idle (no target) for longer than a whole volley: start fresh
        }
        lastTouched = now;
        if (now < pauseUntil) {
            return null;
        }
        if (!inVolley) {
            startVolley(now, ids);
        }
        if (now - volleyStartedAt >= maxTicks) {
            endVolley(now);
            return null;
        }
        if (now < nextShotAt) {
            return null;
        }
        for (int i = 0; i < remaining.size(); i++) {
            String id = remaining.get(i);
            if (readiness.ready(id)) {
                remaining.remove(i);
                nextShotAt = now + gapTicks;
                lastFired = id;
                if (remaining.isEmpty()) {
                    endVolley(now);
                }
                return id;
            }
        }
        return null;
    }

    private void startVolley(long now, Collection<String> ids) {
        remaining.clear();
        remaining.addAll(ids);
        Collections.shuffle(remaining, rnd);
        if (remaining.size() > 1 && remaining.get(0).equals(lastOfPrevious)) {
            int swap = 1 + rnd.nextInt(remaining.size() - 1);
            Collections.swap(remaining, 0, swap);
        }
        inVolley = true;
        volleyStartedAt = now;
        nextShotAt = now;
        lastFired = null;
    }

    private void endVolley(long now) {
        inVolley = false;
        remaining.clear();
        pauseUntil = now + pauseTicks;
        lastOfPrevious = lastFired;
    }

    /** True from the first shot of a volley until its pause begins. */
    public boolean inVolley() {
        return inVolley;
    }

    /** True while the post-volley pause is running. */
    public boolean pausing(long now) {
        return now < pauseUntil;
    }

    /** Foci still to fire in the current volley, in the order they will be tried. */
    public List<String> remaining() {
        return new ArrayList<>(remaining);
    }

    public void reset() {
        remaining.clear();
        inVolley = false;
        pauseUntil = 0L;
        nextShotAt = 0L;
        lastOfPrevious = null;
        lastFired = null;
    }
}
