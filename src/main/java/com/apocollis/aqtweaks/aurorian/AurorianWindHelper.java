package com.apocollis.aqtweaks.aurorian;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Deterministic wind for the Aurorian, the cold twin of {@code AtumWindHelper}. Driven by
 * {@code getTotalWorldTime()} so client and server agree; a different phase from Atum.
 */
public final class AurorianWindHelper {
    public static final double DEFAULT_WIND_ANGLE = 2.35D;

    private AurorianWindHelper() {}

    public static double getWindAngle(World world, boolean isStorming) {
        long t = world.getTotalWorldTime();
        double slowVeer = 0.60D * Math.sin(t * 0.00068D + 0.9D) + 0.35D * Math.sin(t * 0.00029D + 2.6D);
        double gustVeer = isStorming ? (0.40D * Math.sin(t * 0.011D + 1.1D) + 0.22D * Math.cos(t * 0.025D + 0.2D)) : 0.0D;
        return normalizeAngle(DEFAULT_WIND_ANGLE + slowVeer + gustVeer);
    }

    /** Unit vector pointing the way the wind blows. */
    public static Vec3d getWindVector(World world, boolean isStorming) {
        double angle = getWindAngle(world, isStorming);
        return new Vec3d(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    public static double normalizeAngle(double angle) {
        while (angle <= -Math.PI) angle += 2.0D * Math.PI;
        while (angle > Math.PI) angle -= 2.0D * Math.PI;
        return angle;
    }
}
