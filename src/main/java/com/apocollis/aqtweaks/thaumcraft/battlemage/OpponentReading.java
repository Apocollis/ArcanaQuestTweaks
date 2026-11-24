package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;

/**
 * Reading the opponent (plan 3.11 item 4): what a player would notice. A raised shield facing the mage makes projectiles
 * and bolts a waste, so clouds, mines and debuffs are preferred while it is up. {@link #roleScale} is the pure part.
 */
public final class OpponentReading {

    /** Weight scale for single-target shots against a raised shield. */
    public static final double SHOT_SCALE = 0.35D;
    /** Weight scale for area and debuff foci against a raised shield. */
    public static final double AREA_SCALE = 1.6D;

    private OpponentReading() {}

    /** The weight multiplier for a focus role while the target {@code guarded} behind a shield. 1.0 when not guarded. */
    public static double roleScale(String role, boolean guarded) {
        if (!guarded || role == null) {
            return 1.0D;
        }
        switch (role) {
            case "ranged_primary":
            case "ranged_heavy":
            case "bolt":
            case "bolt_heavy":
                return SHOT_SCALE;
            case "aoe":
            case "area_denial":
            case "debuff":
                return AREA_SCALE;
            default:
                return 1.0D;
        }
    }

    /** True when the target holds a vanilla-style shield up and faces the mage. */
    public static boolean shieldRaised(EntityLivingBase target, EntityLivingBase mage) {
        if (target == null || !target.isHandActive() || !target.isActiveItemStackBlocking()) {
            return false;
        }
        Vec3d look = target.getLookVec();
        Vec3d to = new Vec3d(mage.posX - target.posX, 0.0D, mage.posZ - target.posZ).normalize();
        return look.x * to.x + look.z * to.z > 0.5D;
    }
}
