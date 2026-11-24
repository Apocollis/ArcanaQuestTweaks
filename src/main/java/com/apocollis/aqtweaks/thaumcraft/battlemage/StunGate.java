package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * Crowd-control resistance (plan 3.13 item 6): a Stun lasts {@code durationScale} times as long and, once it ends, the
 * mage cannot be stunned again for {@code immunityTicks}. Shock stays a tool, not a lock. Pure and unit tested.
 */
public final class StunGate {

    private long immuneUntil = -1L;

    /** @return the Stun length to apply, or 0 for none */
    public int adjust(long now, int ticks, double durationScale, int immunityTicks) {
        if (ticks <= 0 || durationScale <= 0.0D || now < immuneUntil) {
            return 0;
        }
        int scaled = Math.max(1, (int) Math.round(ticks * durationScale));
        immuneUntil = now + scaled + Math.max(0, immunityTicks);
        return scaled;
    }
}
