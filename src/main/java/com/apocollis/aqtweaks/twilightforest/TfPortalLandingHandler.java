package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.therandomlabs.randomportals.api.event.NetherPortalEvent;
import com.therandomlabs.randomportals.api.netherportal.NetherPortal;
import com.therandomlabs.randomportals.api.netherportal.TeleportData;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import twilightforest.TFTeleporter;
import twilightforest.world.TFWorld;

public class TfPortalLandingHandler {

    public static boolean shouldGate(int dimensionId) {
        var general = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.general;
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
        MinecraftServer server = entity.world.getMinecraftServer();
        if (server == null) {
            return;
        }
        int dim = entity.world.provider.getDimension();
        TFTeleporter teleporter = TFTeleporter.getTeleporterForDim(server, dim);
        boolean checkProgression = TFWorld.isProgressionEnforced(entity.world);
        BlockPos here = new BlockPos(entity);
        if (!isLandingOk(teleporter, entity, here, checkProgression)) {
            BlockPos safe = findSafeCoords(teleporter, entity, here, 200, checkProgression);
            if (safe == null) {
                safe = findSafeCoords(teleporter, entity, here, 400, checkProgression);
            }
            if (safe == null) {
                return;
            }
            here = safe;
        }
        int grassY = TfPortalGrass.findGrassBlockY(entity.world, here.getX(), here.getZ());
        if (grassY == Integer.MIN_VALUE) {
            return;
        }
        entity.setLocationAndAngles(here.getX() + 0.5, grassY + 1, here.getZ() + 0.5,
                entity.rotationYaw, entity.rotationPitch);
    }

    private static boolean isLandingOk(TFTeleporter teleporter, Entity entity, BlockPos pos,
            boolean checkProgression) {
        World world = entity.world;
        if (!teleporter.isSafeAround(pos, entity, checkProgression)) {
            return false;
        }
        if (!TfPortalGrass.biomeAllowed(world.getBiome(pos))) {
            return false;
        }
        return TfPortalGrass.hasGrassColumn(world, pos.getX(), pos.getZ());
    }

    private static BlockPos findSafeCoords(TFTeleporter teleporter, Entity entity, BlockPos origin,
            int range, boolean checkProgression) {
        World world = entity.world;
        int attempts = range / 8;
        for (int i = 0; i < attempts; i++) {
            BlockPos candidate = new BlockPos(
                    origin.getX() + world.rand.nextInt(range) - world.rand.nextInt(range),
                    100,
                    origin.getZ() + world.rand.nextInt(range) - world.rand.nextInt(range));
            if (isLandingOk(teleporter, entity, candidate, checkProgression)) {
                return candidate;
            }
        }
        return null;
    }
}
