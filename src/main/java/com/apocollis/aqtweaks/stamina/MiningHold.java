package com.apocollis.aqtweaks.stamina;

/**
 * The pure rules of hold-based mining stamina (plan 20261120-1700). A dig session starts when the first dig activity
 * arrives after a quiet gap; the start of the session and the start of every later interval are paid. The interval
 * timer is not reset by moving to the next block, so a faster tool, which finishes blocks sooner, pays less per block.
 * No Minecraft types: unit tested.
 */
public final class MiningHold {

    /** One player's session clock (ticks of the server world). */
    public static final class Session {
        long lastTouch = Long.MIN_VALUE / 4;
        long lastPaid = Long.MIN_VALUE / 4;
    }

    private MiningHold() {}

    /**
     * A dig activity at tick {@code now}: the left click that starts hitting a block, or a tick of the server's dig.
     *
     * @param sessionGap more ticks than this since the last activity means a new session (with a start payment)
     * @param interval   ticks between two payments inside a session
     * @return 1 when a payment is due now, else 0
     */
    public static int touch(Session s, long now, int sessionGap, int interval) {
        boolean fresh = now - s.lastTouch > Math.max(1, sessionGap);
        s.lastTouch = now;
        if (fresh || now - s.lastPaid >= Math.max(1, interval)) {
            s.lastPaid = now;
            return 1;
        }
        return 0;
    }

    /** True for a block the hold timer bills (hardness above 0; 0 is a plant with a flat cost, below 0 unbreakable). */
    public static boolean billedByHold(float hardness) {
        return hardness > 0.0F;
    }

    /** True for a block that costs the flat plant price (hardness exactly 0). */
    public static boolean flatPlant(float hardness) {
        return hardness == 0.0F;
    }

    /** Usable stamina in half-feathers: feathers above the armor weight floor plus absorption, never below 0. */
    public static int usable(int feathers, int weight, int absorption) {
        return Math.max(0, feathers - Math.max(0, weight)) + Math.max(0, absorption);
    }

    /** Mining Fatigue applies when the usable stamina is at or below the threshold. */
    public static boolean fatigueDue(int usable, int threshold) {
        return usable <= threshold;
    }
}
