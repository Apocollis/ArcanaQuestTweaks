package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Deterministic procedural wind calculations for the Atum dimension.
 * Synchronized naturally between client and server via world.getTotalWorldTime().
 */
public class AtumWindHelper {
    public static final double DEFAULT_WIND_ANGLE = 0.45D;

    /**
     * Calculates the deterministic wind angle (in radians) for the given world and storm state.
     * World total time is synced by Minecraft's time packet, ensuring client and server stay aligned.
     */
    public static double getWindAngle(World world, boolean isStorming) {
        if (!ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.enableDynamicWindDirection) {
            return DEFAULT_WIND_ANGLE;
        }
        long t = world.getTotalWorldTime();
        // Slow continuous veering over time (~120-180 second cycles)
        double slowVeer = 0.65D * Math.sin(t * 0.00075D) + 0.35D * Math.sin(t * 0.00031D + 1.7D);
        // Storm turbulence veering (~8-15 second cycles)
        double gustVeer = isStorming ? (0.45D * Math.sin(t * 0.012D) + 0.25D * Math.cos(t * 0.027D + 0.5D)) : 0.0D;
        return normalizeAngle(DEFAULT_WIND_ANGLE + slowVeer + gustVeer);
    }

    /**
     * Returns the 2D unit vector pointing in the direction the wind is blowing.
     */
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
