package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * The faked runic shield's recharge rules (plan 3.7), pure: the caller reads the entity's absorption, asks
 * {@link #next}, and writes the result back only when it is higher. Only damage ever lowers the shield, so this class
 * never returns less than it was given.
 *
 * <ul>
 * <li>+1 point every {@code intervalTicks} while below the cap.
 * <li>When the shield reaches 0 it waits {@code breakWaitTicks} before the first point comes back.
 * <li>"Recovering" lasts from the break until the shield is back to {@code resumeFraction} of the cap: the mage keeps its
 *     wider band during that time (plan 3.7).
 * </ul>
 */
public final class ShieldModel {

    private final int intervalTicks;
    private final int breakWaitTicks;
    private final double resumeFraction;

    private long nextPointAt;
    private long waitUntil;
    private boolean broken;
    private boolean recovering;

    public ShieldModel(int intervalTicks, int breakWaitTicks, double resumeFraction) {
        this.intervalTicks = Math.max(1, intervalTicks);
        this.breakWaitTicks = Math.max(0, breakWaitTicks);
        this.resumeFraction = Math.max(0.0D, Math.min(1.0D, resumeFraction));
    }

    /**
     * @param current the shield points now (vanilla absorption)
     * @param max the cap; 0 or less means no shield
     * @param canPay whether a point may be added now (false when the aura drain is on and the chunk has no vis)
     * @return the value to hold from now on: {@code current}, or {@code current + 1} when a point is due
     */
    public double next(long now, double current, double max, boolean canPay) {
        if (max <= 0.0D) {
            return current;
        }
        if (current <= 0.0D) {
            if (!broken) {
                broken = true;
                recovering = true;
                waitUntil = now + breakWaitTicks;
                nextPointAt = waitUntil;
            }
        } else {
            broken = false;
        }
        if (current >= max) {
            if (recovering && current >= max * resumeFraction) {
                recovering = false;
            }
            return current;
        }
        if (now < waitUntil || now < nextPointAt) {
            return current;
        }
        if (!canPay) {
            nextPointAt = now + intervalTicks;
            return current;
        }
        double value = Math.min(max, current + 1.0D);
        nextPointAt = now + intervalTicks;
        if (recovering && value >= max * resumeFraction) {
            recovering = false;
        }
        return value;
    }

    /** True from the moment the shield hit 0 until it is back to the resume fraction: widen the band, favour peel. */
    public boolean widenBand(double current, double max) {
        if (max <= 0.0D) {
            return false;
        }
        return current <= 0.0D || (recovering && current < max * resumeFraction);
    }

    /** True while the shield is at 0. */
    public boolean isBroken(double current, double max) {
        return max > 0.0D && current <= 0.0D;
    }
}
