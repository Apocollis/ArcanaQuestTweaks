package com.apocollis.aqtweaks.mixin.randomportals;

import com.apocollis.aqtweaks.twilightforest.TfPortalGrass;
import com.apocollis.aqtweaks.twilightforest.TfPortalLandingHandler;
import com.therandomlabs.randomportals.api.config.PortalType;
import com.therandomlabs.randomportals.api.frame.FrameType;
import com.therandomlabs.randomportals.config.RPOConfig;
import com.therandomlabs.randomportals.world.RPOTeleporter;
import com.therandomlabs.randomportals.world.storage.RPOSavedData;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RPOTeleporter.class, remap = false)
public abstract class MixinRPOTeleporter {

    @Shadow
    @Final
    private int dimensionID;

    @Shadow
    public abstract boolean isValidPortalPosition(BlockPos.MutableBlockPos pos, int x, int y, int z,
            int platformWidth, int platformLength, int spaceHeight, FrameType type);

    @Inject(method = "isValidPortalPosition", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$requireGrassPad(BlockPos.MutableBlockPos pos, int x, int y, int z,
            int platformWidth, int platformLength, int spaceHeight, FrameType type,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || !TfPortalLandingHandler.shouldGate(this.dimensionID)) {
            return;
        }
        World world = aqtweaks$destWorld(null);
        if (world == null || !TfPortalGrass.platformIsGrass(world, x, y - 1, z, platformWidth, platformLength,
                type == FrameType.VERTICAL_Z)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "findTopLeft", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$replaceCanopyFallback(RPOSavedData savedData, Entity entity, PortalType portalType,
            FrameType type, int width, int height, IBlockState air, CallbackInfoReturnable<BlockPos> cir) {
        if (!TfPortalLandingHandler.shouldGate(this.dimensionID)) {
            return;
        }
        BlockPos topLeft = cir.getReturnValue();
        if (topLeft == null) {
            return;
        }
        if (aqtweaks$frameTopLeftPlatformIsGrass(topLeft, type, width, height)) {
            return;
        }
        BlockPos grass = aqtweaks$findGrassTopLeft(entity, type, width, height);
        if (grass != null) {
            cir.setReturnValue(grass);
        }
    }

    private boolean aqtweaks$frameTopLeftPlatformIsGrass(BlockPos topLeft, FrameType type, int width, int height) {
        int platformWidth = width;
        int platformLength = type == FrameType.LATERAL ? height : 3;
        int originX;
        int originY;
        int originZ;
        if (type == FrameType.LATERAL) {
            originX = topLeft.getX();
            originY = topLeft.getY();
            originZ = topLeft.getZ();
        } else if (type == FrameType.VERTICAL_Z) {
            originX = topLeft.getX() - 1;
            originY = topLeft.getY() - (height - 2) - 1;
            originZ = topLeft.getZ() - (width - 1);
        } else {
            originX = topLeft.getX();
            originY = topLeft.getY() - (height - 2) - 1;
            originZ = topLeft.getZ() - 1;
        }
        World world = aqtweaks$destWorld(null);
        if (world == null) {
            return false;
        }
        return TfPortalGrass.platformIsGrass(world, originX, originY, originZ, platformWidth, platformLength,
                type == FrameType.VERTICAL_Z);
    }

    private BlockPos aqtweaks$findGrassTopLeft(Entity entity, FrameType type, int width, int height) {
        World world = aqtweaks$destWorld(entity);
        if (world == null) {
            return null;
        }
        int platformWidth = width;
        int platformLength = type == FrameType.LATERAL ? height : 3;
        int spaceHeight = type == FrameType.LATERAL ? 2 : height;
        int radius = RPOConfig.NetherPortals.portalGenerationLocationSearchRadius;
        int entityX = (int) entity.posX;
        int entityZ = (int) entity.posZ;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = entityX - radius; x <= entityX + radius; x++) {
            for (int z = entityZ - radius; z <= entityZ + radius; z++) {
                int grassY = TfPortalGrass.findGrassBlockY(world, x, z);
                if (grassY == Integer.MIN_VALUE) {
                    continue;
                }
                int y = grassY + 1;
                if (!this.isValidPortalPosition(pos, x, y, z, platformWidth, platformLength, spaceHeight, type)) {
                    continue;
                }
                double dist = entity.getDistanceSq(x + 0.5, y, z + 0.5);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = aqtweaks$toTopLeft(x, y, z, type, width, height);
                }
            }
        }
        return best;
    }

    private static BlockPos aqtweaks$toTopLeft(int portalX, int portalY, int portalZ, FrameType type, int width,
            int height) {
        BlockPos topLeft = new BlockPos(portalX, portalY, portalZ);
        if (type == FrameType.LATERAL) {
            return topLeft.down();
        }
        topLeft = topLeft.up(height - 2)
                .offset(type == FrameType.VERTICAL_Z ? EnumFacing.EAST : EnumFacing.SOUTH);
        if (type == FrameType.VERTICAL_Z) {
            topLeft = topLeft.south(width - 1);
        }
        return topLeft;
    }

    /** Do not @Shadow Teleporter.world: remap=false targets RPOTeleporter, which has no field_85192_a. */
    private World aqtweaks$destWorld(Entity entity) {
        if (entity != null && entity.world != null) {
            return entity.world;
        }
        return DimensionManager.getWorld(this.dimensionID);
    }
}
