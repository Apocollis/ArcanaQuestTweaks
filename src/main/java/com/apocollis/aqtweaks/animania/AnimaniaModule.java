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

public class AnimaniaModule {

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null || entity.world.isRemote) return;
        if (!(entity instanceof IFoodEating eating)) return;
        if (!eating.getHandFed() && !eating.getInteracted()) return;
        if (!rancherNearby(entity)) return;

        if (entity instanceof IImpregnable pregnant) {
            extraGestation(pregnant);
            extraDry(pregnant);
        }
        if (entity instanceof IChild child) {
            child.setAgeTimer(child.getAgeTimer() + 1);
        }
        if (!eating.getWatered()) {
            eating.setWaterTimer(eating.getWaterTimer() + 1);
        }
        extraFarmClocks(entity);
    }

    private static void extraFarmClocks(Entity entity) {
        try {
            Class.forName("com.apocollis.aqtweaks.animania.AnimaniaFarmClocks")
                    .getMethod("extraProduction", Entity.class)
                    .invoke(null, entity);
        } catch (Throwable ignored) {
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

    private static void extraGestation(IImpregnable animal) {
        if (!animal.getPregnant()) return;
        int g = animal.getGestation();
        if (g > -1) {
            animal.setGestation(g - 1);
        }
    }

    private static void extraDry(IImpregnable animal) {
        if (animal.getFertile()) return;
        int dry = animal.getDryTimer();
        if (dry > -1) {
            animal.setDryTimer(dry - 1);
        }
    }
}
