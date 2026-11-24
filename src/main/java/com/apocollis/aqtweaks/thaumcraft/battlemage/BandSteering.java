package com.apocollis.aqtweaks.thaumcraft.battlemage;

/** The movement decision of the stage 1 AI: hold the preferred band to the target. */
public final class BandSteering {

    public enum Move { APPROACH, RETREAT, STRAFE, HOLD }

    private BandSteering() {}

    /**
     * @param distance distance to the target in blocks
     * @param idealMin inner edge of the preferred band
     * @param idealMax outer edge of the preferred band
     * @param hasLos   whether the mage can see the target
     */
    public static Move decide(double distance, double idealMin, double idealMax, boolean hasLos) {
        if (distance < idealMin) {
            return Move.RETREAT;
        }
        if (!hasLos || distance > idealMax) {
            return Move.APPROACH;
        }
        return Move.STRAFE;
    }
}
