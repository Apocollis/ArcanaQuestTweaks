package com.apocollis.aqtweaks.animania;

import com.animania.api.interfaces.IChild;
import com.animania.api.interfaces.IFoodEating;
import com.animania.api.interfaces.IImpregnable;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;

public class AnimaniaModule {

    private static final int RANCHER_THROTTLE = 20;

    private static Method extraProductionBulkMethod;
    private static boolean farmClocksResolved;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null || entity.world.isRemote) return;
        if (!(entity instanceof IFoodEating eating)) return;
        if (!eating.getHandFed() && !eating.getInteracted()) return;
        if (entity.ticksExisted % RANCHER_THROTTLE != 0) return;
        if (!rancherNearby(entity)) return;

        if (entity instanceof IImpregnable pregnant) {
            extraGestationBulk(pregnant, RANCHER_THROTTLE);
            extraDryBulk(pregnant, RANCHER_THROTTLE);
        }
        if (entity instanceof IChild child) {
            child.setAgeTimer(child.getAgeTimer() + RANCHER_THROTTLE);
        }
        if (!eating.getWatered()) {
            eating.setWaterTimer(eating.getWaterTimer() + RANCHER_THROTTLE);
        }
        extraFarmClocksBulk(entity, RANCHER_THROTTLE);
    }

    private static void extraFarmClocksBulk(Entity entity, int steps) {
        if (!farmClocksResolved) {
            farmClocksResolved = true;
            try {
                extraProductionBulkMethod = Class.forName("com.apocollis.aqtweaks.animania.AnimaniaFarmClocks")
                        .getMethod("extraProductionBulk", Entity.class, int.class);
            } catch (Throwable ignored) {
            }
        }
        if (extraProductionBulkMethod != null) {
            try {
                extraProductionBulkMethod.invoke(null, entity, steps);
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean rancherNearby(Entity entity) {
        World world = entity.world;
        if (world == null) return false;
        double range = ArcanaQuestTweaksConfig.ReskillableModuleConfig.farming.rancherRange;
        AxisAlignedBB box = entity.getEntityBoundingBox().grow(range);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, box)) {
            if (player instanceof FakePlayer) continue;
            if (player.getDistanceSq(entity) > range * range) continue;
            if (Reflect.hasUnlockable(player, "aqtweaks:rancher")) return true;
        }
        return false;
    }

    private static void extraGestationBulk(IImpregnable animal, int steps) {
        if (!animal.getPregnant()) return;
        int g = animal.getGestation();
        if (g > 1) animal.setGestation(Math.max(g - steps, 1));
    }

    private static void extraDryBulk(IImpregnable animal, int steps) {
        if (animal.getFertile()) return;
        int dry = animal.getDryTimer();
        if (dry > -1) animal.setDryTimer(Math.max(dry - steps, -1));
    }
}
