package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * Sidestep decision (plan 3.5): is a projectile heading at the mage, and which way is perpendicular? Pure vector maths.
 * It only reacts to what a player could see: a projectile on a collision course close by, or a drawn bow aimed at it.
 */
public final class DodgePlanner {

    /** Result: 0 for no dodge, otherwise the sign (+1 or -1) of the perpendicular to step along. */
    private DodgePlanner() {}

    /**
     * @param px projectile position
     * @param pvx projectile velocity per tick
     * @param mx mage position (centre of the body)
     * @param hitRadius how close the closest approach must be to count as a hit
     * @param maxTicks only react when the impact is this many ticks away or less
     * @return {@code 0} when it will not hit, otherwise {@code +1} or {@code -1}: the side to step to (away from the closest approach)
     */
    public static int sideFor(double px, double py, double pz, double pvx, double pvy, double pvz, double mx, double my,
            double mz, double hitRadius, int maxTicks) {
        double speed2 = pvx * pvx + pvy * pvy + pvz * pvz;
        if (speed2 < 1.0E-6D) {
            return 0;
        }
        double rx = mx - px;
        double ry = my - py;
        double rz = mz - pz;
        double t = (rx * pvx + ry * pvy + rz * pvz) / speed2;
        if (t < 0.0D || t > maxTicks) {
            return 0;
        }
        double cx = px + pvx * t - mx;
        double cy = py + pvy * t - my;
        double cz = pz + pvz * t - mz;
        if (cx * cx + cy * cy + cz * cz > hitRadius * hitRadius) {
            return 0;
        }
        // Horizontal perpendicular of the velocity; step to the side the closest approach is NOT on (away from the line).
        double perpX = -pvz;
        double perpZ = pvx;
        double side = cx * perpX + cz * perpZ;
        return side > 0.0D ? -1 : 1;
    }

    /** The horizontal unit perpendicular of a direction (x, z), as {x, z}. */
    public static double[] perpendicular(double dx, double dz, int sign) {
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-6D) {
            return new double[] {sign, 0.0D};
        }
        return new double[] {-dz / len * sign, dx / len * sign};
    }
}
