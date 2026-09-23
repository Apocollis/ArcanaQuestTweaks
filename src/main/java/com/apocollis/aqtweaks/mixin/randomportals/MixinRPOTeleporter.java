package com.apocollis.aqtweaks.mixin.randomportals;

import com.apocollis.aqtweaks.aether.AetherPortalIsland;
import com.apocollis.aqtweaks.aether.AetherPortalLandingHandler;
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
    private void aqtweaks$requireSafePad(BlockPos.MutableBlockPos pos, int x, int y, int z,
            int platformWidth, int platformLength, int spaceHeight, FrameType type,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        boolean tf = TfPortalLandingHandler.shouldGate(this.dimensionID);
        boolean aether = AetherPortalLandingHandler.shouldGate(this.dimensionID);
        if (!tf && !aether) {
            return;
        }
        World world = aqtweaks$destWorld(null);
        boolean verticalZ = type == FrameType.VERTICAL_Z;
        boolean ok = world != null && (tf
                ? TfPortalGrass.platformIsGrass(world, x, y - 1, z, platformWidth, platformLength, verticalZ)
                : AetherPortalIsland.platformIsIsland(world, x, y - 1, z, platformWidth, platformLength, verticalZ));
        if (ok && type != FrameType.LATERAL) {
            ok = aqtweaks$airAboveFrame(world, x, y, z, platformWidth, platformLength, spaceHeight, verticalZ);
        }
        if (!ok) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "findTopLeft", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$replaceVoidFallback(RPOSavedData savedData, Entity entity, PortalType portalType,
            FrameType type, int width, int height, IBlockState air, CallbackInfoReturnable<BlockPos> cir) {
        boolean tf = TfPortalLandingHandler.shouldGate(this.dimensionID);
        boolean aether = AetherPortalLandingHandler.shouldGate(this.dimensionID);
        if (!tf && !aether) {
            return;
        }
        BlockPos topLeft = cir.getReturnValue();
        if (topLeft == null) {
            return;
        }
        if (tf) {
            if (aqtweaks$frameTopLeftPlatformIsGrass(topLeft, type, width, height)) {
                cir.setReturnValue(aqtweaks$sitOnSurface(topLeft, type));
                return;
            }
            BlockPos grass = aqtweaks$findSurfaceTopLeft(entity, type, width, height, true);
            if (grass != null) {
                cir.setReturnValue(grass);
            }
            return;
        }
        if (aqtweaks$frameTopLeftPlatformIsIsland(topLeft, type, width, height)) {
            cir.setReturnValue(aqtweaks$sitOnSurface(topLeft, type));
            return;
        }
        BlockPos island = aqtweaks$findSurfaceTopLeft(entity, type, width, height, false);
        if (island != null) {
            cir.setReturnValue(island);
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

    private boolean aqtweaks$frameTopLeftPlatformIsIsland(BlockPos topLeft, FrameType type, int width, int height) {
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
        return AetherPortalIsland.platformIsIsland(world, originX, originY, originZ, platformWidth, platformLength,
                type == FrameType.VERTICAL_Z);
    }

    private BlockPos aqtweaks$findSurfaceTopLeft(Entity entity, FrameType type, int width, int height, boolean twilight) {
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
                int surfaceY = twilight
                        ? TfPortalGrass.findGrassBlockY(world, x, z)
                        : AetherPortalIsland.findSurfaceY(world, x, z);
                if (surfaceY == Integer.MIN_VALUE) {
                    continue;
                }
                int y = surfaceY + 1;
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
        return aqtweaks$sitOnSurface(topLeft, type);
    }

    /**
     * RP places a vertical frame's bottom row in the solid platform (the grass).
     * Shift up so that row sits on the surface instead.
     */
    private static BlockPos aqtweaks$sitOnSurface(BlockPos topLeft, FrameType type) {
        return type == FrameType.LATERAL ? topLeft : topLeft.up();
    }

    private static boolean aqtweaks$airAboveFrame(World world, int x, int y, int z,
            int platformWidth, int platformLength, int spaceHeight, boolean verticalZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int widthOffset = 0; widthOffset < platformWidth; widthOffset++) {
            for (int lengthOffset = 0; lengthOffset < platformLength; lengthOffset++) {
                int ox = verticalZ ? lengthOffset : widthOffset;
                int oz = verticalZ ? widthOffset : lengthOffset;
                if (!world.isAirBlock(pos.setPos(x + ox, y + spaceHeight, z + oz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Do not @Shadow Teleporter.world: remap=false targets RPOTeleporter, which has no field_85192_a. */
    private World aqtweaks$destWorld(Entity entity) {
        if (entity != null && entity.world != null) {
            return entity.world;
        }
        return DimensionManager.getWorld(this.dimensionID);
    }
}
