package com.apocollis.aqtweaks.aether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.therandomlabs.randomportals.api.event.NetherPortalEvent;
import com.therandomlabs.randomportals.api.netherportal.NetherPortal;
import com.therandomlabs.randomportals.api.netherportal.TeleportData;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AetherPortalLandingHandler {

    public static boolean shouldGate(int dimensionId) {
        var general = ArcanaQuestTweaksConfig.AetherModuleConfig.general;
        return general.enable && dimensionId == general.destinationDimensionId;
    }

    @SubscribeEvent
    public void onSearchingForDestination(NetherPortalEvent.Teleport.SearchingForDestination event) {
        Entity entity = event.getEntity();
        if (entity == null || entity.world == null || entity.world.isRemote) {
            return;
        }
        if (!shouldGate(entity.world.provider.getDimension())) {
            return;
        }
        TeleportData data = event.getData();
        NetherPortal sending = data == null ? null : data.getPortal();
        if (sending != null && sending.getReceivingFrame() != null) {
            return;
        }
        World world = entity.world;
        BlockPos here = new BlockPos(entity);
        if (!AetherPortalIsland.hasIslandColumn(world, here.getX(), here.getZ())) {
            BlockPos island = findIsland(world, here, 200);
            if (island == null) {
                island = findIsland(world, here, 400);
            }
            if (island == null) {
                return;
            }
            here = island;
        }
        int surfaceY = AetherPortalIsland.findSurfaceY(world, here.getX(), here.getZ());
        if (surfaceY == Integer.MIN_VALUE) {
            return;
        }
        entity.setLocationAndAngles(here.getX() + 0.5, surfaceY + 1, here.getZ() + 0.5,
                entity.rotationYaw, entity.rotationPitch);
    }

    private static BlockPos findIsland(World world, BlockPos origin, int range) {
        int attempts = range / 8;
        for (int i = 0; i < attempts; i++) {
            int x = origin.getX() + world.rand.nextInt(range) - world.rand.nextInt(range);
            int z = origin.getZ() + world.rand.nextInt(range) - world.rand.nextInt(range);
            if (AetherPortalIsland.hasIslandColumn(world, x, z)) {
                return new BlockPos(x, 0, z);
            }
        }
        return null;
    }
}
