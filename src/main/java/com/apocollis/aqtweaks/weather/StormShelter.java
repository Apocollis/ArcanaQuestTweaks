package com.apocollis.aqtweaks.weather;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Wind shelter test shared by the Atum sandstorm and the Aurorian Moonfrost storm. */
public final class StormShelter {
    private StormShelter() {}

    /**
     * Sky test that ignores foliage: true if the position sees the sky, or every block above it up to the
     * height map is air, leaves or otherwise non-opaque. Stone, planks and other real roofs still count.
     */
    public static boolean openSky(World world, BlockPos pos) {
        if (world.canSeeSky(pos)) {
            return true;
        }
        int top = world.getHeight(pos).getY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = pos.getY() + 1; y <= top; y++) {
            m.setPos(pos.getX(), y, pos.getZ());
            IBlockState state = world.getBlockState(m);
            if (state.getLightOpacity(world, m) <= 0) continue;
            if (state.getMaterial() == Material.LEAVES || state.getBlock().isLeaves(state, world, m)) continue;
            return false;
        }
        return true;
    }

    /**
     * True when the entity cannot see the sky, or when an upwind block blocks the wind at both the feet and
     * the chest (a dune, wall, pyramid step or cliff).
     */
    public static boolean isSheltered(EntityLivingBase entity, World world, Vec3d windVec, double shelterDist) {
        return isSheltered(entity, world, windVec, shelterDist, false);
    }

    /**
     * Same test; with {@code ignoreLeaves} a canopy of leaves above the eye does not count as a roof (see
     * {@link #openSky}). Atum passes false and is unchanged.
     */
    public static boolean isSheltered(EntityLivingBase entity, World world, Vec3d windVec, double shelterDist,
                                      boolean ignoreLeaves) {
        BlockPos eyePos = new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        boolean open = ignoreLeaves ? openSky(world, eyePos) : world.canSeeSky(eyePos);
        if (!open) {
            return true; // Indoors or underground
        }

        // Check feet height upwind (bottom of model)
        Vec3d feetVec = new Vec3d(entity.posX, entity.posY + Math.min(0.25D, entity.height * 0.2D), entity.posZ);
        Vec3d upFeet = new Vec3d(feetVec.x - windVec.x * shelterDist, feetVec.y, feetVec.z - windVec.z * shelterDist);
        RayTraceResult rayFeet = world.rayTraceBlocks(upFeet, feetVec, false, true, false);
        if (rayFeet == null || rayFeet.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false; // Feet exposed to wind (e.g. gap or floating overhang)
        }

        // Check chest height upwind (top barrier in front must also block chest / upper body)
        Vec3d chestVec = new Vec3d(entity.posX, entity.posY + entity.height * 0.70D, entity.posZ);
        Vec3d upChest = new Vec3d(chestVec.x - windVec.x * shelterDist, chestVec.y, chestVec.z - windVec.z * shelterDist);
        RayTraceResult rayChest = world.rayTraceBlocks(upChest, chestVec, false, true, false);
        return rayChest != null && rayChest.typeOfHit == RayTraceResult.Type.BLOCK;
    }
}
