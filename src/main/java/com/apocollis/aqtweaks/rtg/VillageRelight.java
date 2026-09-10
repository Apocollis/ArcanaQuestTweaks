package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

/**
 * Village pieces paste with flag 2, so torch/lamp block light often never floods.
 * After that chunk's paste, re-check light at every emitting block in the clip.
 */
public final class VillageRelight {

    private VillageRelight() {}

    public static void afterVillagePaste(World world, StructureBoundingBox clip) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageRelight) return;
        if (world == null || clip == null || world.isRemote) return;
        int[] box = new int[] {clip.minX, clip.maxX, clip.minY, clip.maxY, clip.minZ, clip.maxZ};
        if (box == null) return;
        int minX = box[0];
        int maxX = box[1];
        int minY = Math.max(0, box[2]);
        int maxY = Math.min(255, box[3]);
        int minZ = box[4];
        int maxZ = box[5];
        if (minX > maxX || minY > maxY || minZ > maxZ) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int lit = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.setPos(x, y, z);
                    if (!world.isBlockLoaded(pos)) continue;
                    IBlockState state = world.getBlockState(pos);
                    if (state == null || lightValue(state, world, pos) <= 0) continue;
                    world.checkLight(pos);
                    lit++;
                }
            }
        }
        if (lit > 0) {
            world.markBlockRangeForRenderUpdate(minX, minY, minZ, maxX, maxY, maxZ);
            if (VillageDebug.enabled()) {
                VillageDebug.log("relight clip=[%d,%d]x[%d,%d] y=%d..%d sources=%d",
                        minX, maxX, minZ, maxZ, minY, maxY, lit);
            }
        }
    }

    private static int lightValue(IBlockState state, World world, BlockPos pos) {
        try {
            return state.getLightValue(world, pos);
        } catch (Throwable ignored) {}
        try {
            return state.getLightValue();
        } catch (Throwable ignored) {}
        return 0;
    }
}
