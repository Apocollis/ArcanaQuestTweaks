package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.PortalModuleConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

public final class RiftTeleporter implements ITeleporter {

    private final double x;
    private final double y;
    private final double z;
    private final float yaw;

    public RiftTeleporter(double x, double y, double z, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
    }

    @Override
    public void placeEntity(World world, Entity entity, float rotationYaw) {
        entity.motionX = 0.0;
        entity.motionY = 0.0;
        entity.motionZ = 0.0;
        entity.fallDistance = 0.0F;
        entity.timeUntilPortal = PortalModuleConfig.general.cooldownTicks;
        if (entity instanceof EntityPlayerMP mp) {
            mp.connection.setPlayerLocation(x, y, z, yaw, entity.rotationPitch);
        } else {
            entity.setLocationAndAngles(x, y, z, yaw, entity.rotationPitch);
        }
    }

    @Override
    public boolean isVanilla() {
        return false;
    }
}
