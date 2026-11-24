package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;

/** Aims a copied package at a target by replacing its ROOT medium (plan 2.1: setupFromCasterToTarget with lead). */
public final class BattlemageAim {

    private BattlemageAim() {}

    public static void aim(EntityLivingBase caster, FocusPackage pkg, EntityLivingBase target, FocusDef def, AiTuning ai) {
        Vec3d source = caster.getPositionEyes(1.0F);
        Vec3d point = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
        if (ai.leadShots && isProjectileRole(def.role)) {
            double dist = source.distanceTo(point);
            double ticks = Math.min(ai.maxLeadTicks, dist / ai.projectileSpeed);
            point = point.add((target.posX - target.lastTickPosX) * ticks, 0.0D, (target.posZ - target.lastTickPosZ) * ticks);
        }
        Vec3d dir = point.subtract(source).normalize();
        FocusMediumRoot root = new FocusMediumRoot(new Trajectory[] {new Trajectory(source, dir)},
                new RayTraceResult[] {new RayTraceResult(target)});
        if (!pkg.nodes.isEmpty() && pkg.nodes.get(0) instanceof FocusMediumRoot) {
            pkg.nodes.set(0, root);
        } else {
            pkg.nodes.add(0, root);
        }
    }

    static boolean isProjectileRole(String role) {
        return "ranged_primary".equals(role) || "ranged_heavy".equals(role);
    }
}
