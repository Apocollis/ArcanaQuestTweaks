package com.apocollis.aqtweaks.starter.client;

/**
 * The reveal panel's life: fade in, hold, fade out, done. Pure tick logic so it can be tested. Continue (click or
 * Escape) skips the rest of the hold; the hold also ends by itself after {@code holdTicks}.
 */
public final class RevealTimeline {

    public enum Stage { FADE_IN, HOLD, FADE_OUT, DONE }

    private final int fadeTicks;
    private final int holdTicks;
    private int ticks;
    private int outStart = -1;

    public RevealTimeline(int fadeTicks, int holdTicks) {
        this.fadeTicks = Math.max(1, fadeTicks);
        this.holdTicks = Math.max(0, holdTicks);
    }

    public void tick() {
        if (stage() != Stage.DONE) {
            ticks++;
        }
    }

    /** Continue clicked: start fading out now (only once the fade in has finished or at once if still fading in). */
    public void continueClicked() {
        if (outStart < 0) {
            outStart = Math.max(ticks, fadeTicks);
        }
    }

    public Stage stage() {
        int hold = fadeTicks + holdTicks;
        int start = outStart >= 0 ? Math.min(outStart, hold) : hold;
        if (ticks < fadeTicks && outStart < 0) {
            return Stage.FADE_IN;
        }
        if (ticks < start) {
            return ticks < fadeTicks ? Stage.FADE_IN : Stage.HOLD;
        }
        if (ticks < start + fadeTicks) {
            return Stage.FADE_OUT;
        }
        return Stage.DONE;
    }

    /** Panel alpha in [0, 1] with the partial tick for smoothness. */
    public float alpha(float partial) {
        int hold = fadeTicks + holdTicks;
        int start = outStart >= 0 ? Math.min(outStart, hold) : hold;
        float t = ticks + partial;
        float a;
        if (t < fadeTicks && ticks < start) {
            a = t / fadeTicks;
        } else if (t < start) {
            a = 1f;
        } else {
            a = 1f - (t - start) / fadeTicks;
        }
        return Math.max(0f, Math.min(1f, a));
    }

    public boolean done() {
        return stage() == Stage.DONE;
    }

    public int ticks() {
        return ticks;
    }
}
