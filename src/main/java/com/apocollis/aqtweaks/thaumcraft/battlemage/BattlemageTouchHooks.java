package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.items.casters.foci.FocusMediumBolt;
import thaumcraft.common.items.casters.foci.FocusMediumTouch;
import thaumcraft.common.lib.utils.EntityUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Touch and Bolt for a non-player caster. This is a copy of {@code FocusMediumTouch.supplyTrajectories} and
 * {@code supplyTargets} (BETA26 bytecode) with the player-only reach replaced: 16 blocks for Bolt (the stock
 * constant), {@code Battlemage Touch Reach} for Touch. Players never reach this code.
 */
public final class BattlemageTouchHooks {

    private BattlemageTouchHooks() {}

    public static boolean isNonPlayerCaster(FocusNode medium) {
        if (medium.getPackage() == null || medium.getParent() == null) {
            return false;
        }
        EntityLivingBase caster = medium.getPackage().getCaster();
        return caster != null && !(caster instanceof net.minecraft.entity.player.EntityPlayer);
    }

    private static double reach(FocusMediumTouch medium) {
        if (medium instanceof FocusMediumBolt) {
            return 16.0D;
        }
        return ArcanaQuestTweaksConfig.BattlemageConfig.touchReach;
    }

    public static Trajectory[] trajectories(FocusMediumTouch medium) {
        EntityLivingBase caster = medium.getPackage().getCaster();
        World world = medium.getPackage().world;
        double reach = reach(medium);
        List<Trajectory> out = new ArrayList<>();
        for (Trajectory t : medium.getParent().supplyTrajectories()) {
            Vec3d dir = t.direction.normalize();
            Vec3d end = dir;
            RayTraceResult hit = EntityUtils.getPointedEntityRay(world, caster, t.source, dir, 0.25D, reach, 0.25F, false);
            if (hit == null) {
                end = dir.scale(reach).add(t.source);
                hit = world.rayTraceBlocks(t.source, end);
                if (hit != null) {
                    end = hit.hitVec;
                }
            } else if (hit.entityHit != null) {
                end = dir.scale(t.source.distanceTo(hit.entityHit.getPositionVector())).add(t.source);
            }
            out.add(new Trajectory(end, t.direction.normalize()));
        }
        return out.toArray(new Trajectory[0]);
    }

    public static RayTraceResult[] targets(FocusMediumTouch medium) {
        EntityLivingBase caster = medium.getPackage().getCaster();
        World world = medium.getPackage().world;
        double reach = reach(medium);
        List<RayTraceResult> out = new ArrayList<>();
        for (Trajectory t : medium.getParent().supplyTrajectories()) {
            Vec3d dir = t.direction.normalize();
            RayTraceResult hit = EntityUtils.getPointedEntityRay(world, caster, t.source, dir, 0.25D, reach, 0.25F, false);
            if (hit == null) {
                Vec3d end = dir.scale(reach).add(t.source);
                hit = world.rayTraceBlocks(t.source, end);
            }
            if (hit != null) {
                out.add(hit);
            }
        }
        return out.toArray(new RayTraceResult[0]);
    }
}
