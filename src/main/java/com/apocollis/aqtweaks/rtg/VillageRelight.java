package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

/**
 * Village pieces paste with flag 2, so torch/lamp block light often never floods.
 * After that chunk's paste, re-check light at emitting blocks in each placing component clip.
 */
public final class VillageRelight {

    private VillageRelight() {}

    public static void afterVillagePaste(World world, StructureStart start, StructureBoundingBox clip) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableVillageRelight) return;
        if (world == null || clip == null || world.isRemote) return;

        List<StructureComponent> components = start != null ? start.getComponents() : null;
        if (components == null || components.isEmpty()) return;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Set<Long> checked = new HashSet<>();
        int lit = 0;
        int unionMinX = Integer.MAX_VALUE;
        int unionMaxX = Integer.MIN_VALUE;
        int unionMinY = Integer.MAX_VALUE;
        int unionMaxY = Integer.MIN_VALUE;
        int unionMinZ = Integer.MAX_VALUE;
        int unionMaxZ = Integer.MIN_VALUE;

        for (StructureComponent component : components) {
            if (component == null || component instanceof VillagePieceVillagePlate) continue;
            StructureBoundingBox bb = component.getBoundingBox();
            if (bb == null) continue;
            int minX = Math.max(clip.minX, bb.minX);
            int maxX = Math.min(clip.maxX, bb.maxX);
            int minY = Math.max(0, Math.max(clip.minY, bb.minY));
            int maxY = Math.min(255, Math.min(clip.maxY, bb.maxY));
            int minZ = Math.max(clip.minZ, bb.minZ);
            int maxZ = Math.min(clip.maxZ, bb.maxZ);
            if (minX > maxX || minY > maxY || minZ > maxZ) continue;

            unionMinX = Math.min(unionMinX, minX);
            unionMaxX = Math.max(unionMaxX, maxX);
            unionMinY = Math.min(unionMinY, minY);
            unionMaxY = Math.max(unionMaxY, maxY);
            unionMinZ = Math.min(unionMinZ, minZ);
            unionMaxZ = Math.max(unionMaxZ, maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        pos.setPos(x, y, z);
                        if (!world.isBlockLoaded(pos)) continue;
                        IBlockState state = world.getBlockState(pos);
                        if (state == null || lightValue(state, world, pos) <= 0) continue;
                        if (!checked.add(pos.toLong())) continue;
                        world.checkLight(pos);
                        lit++;
                    }
                }
            }
        }

        if (lit > 0 && unionMinX <= unionMaxX) {
            world.markBlockRangeForRenderUpdate(
                    unionMinX, unionMinY, unionMinZ, unionMaxX, unionMaxY, unionMaxZ);
            if (VillageDebug.enabled()) {
                VillageDebug.log("relight clip=[%d,%d]x[%d,%d] y=%d..%d sources=%d",
                        unionMinX, unionMaxX, unionMinZ, unionMaxZ, unionMinY, unionMaxY, lit);
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
