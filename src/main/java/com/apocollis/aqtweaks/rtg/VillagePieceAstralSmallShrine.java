package com.apocollis.aqtweaks.rtg;

import hellfirepvp.astralsorcery.common.lib.MultiBlockArrays;
import hellfirepvp.astralsorcery.common.structure.array.BlockArray;
import hellfirepvp.astralsorcery.common.structure.array.StructureBlockArray;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Village component that pastes Astral Sorcery's small shrine. At most one per village.
 */
public class VillagePieceAstralSmallShrine extends StructureVillagePieces.Village {

    public VillagePieceAstralSmallShrine() {}

    public VillagePieceAstralSmallShrine(StructureVillagePieces.Start start, int type,
                                        StructureBoundingBox box, EnumFacing facing) {
        super(start, type);
        this.setCoordBaseMode(facing);
        this.boundingBox = box;
    }

    public static VillagePieceAstralSmallShrine build(StructureVillagePieces.Start start,
                                                      List<StructureComponent> pieces, Random random,
                                                      int x, int y, int z, EnumFacing facing, int type) {
        StructureBlockArray template = template();
        if (template == null || template.getSize() == null) return null;
        Vec3i size = template.getSize();
        int footprint = Math.max(1, Math.max(size.getX(), size.getZ()));
        int height = Math.max(1, size.getY());
        StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(
                x, y, z, 0, 0, 0, footprint, height, footprint, facing);
        if (!canVillageGoDeeper(box) || StructureComponent.findIntersecting(pieces, box) != null) {
            return null;
        }
        return new VillagePieceAstralSmallShrine(start, type, box, facing);
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox structurebb) {
        StructureBlockArray template = template();
        if (template == null || template.getMin() == null || template.getPattern() == null) return true;
        Vec3i min = template.getMin();

        if (this.averageGroundLvl < 0) {
            this.averageGroundLvl = this.getAverageGroundLevel(world, structurebb);
            if (this.averageGroundLvl < 0) return true;
            this.boundingBox.offset(0, this.averageGroundLvl - (this.boundingBox.minY - min.getY()), 0);
        }

        if (world.isRemote) return true;
        BlockPos origin = new BlockPos(
                this.boundingBox.minX - min.getX(),
                this.boundingBox.minY - min.getY(),
                this.boundingBox.minZ - min.getZ());
        Map<BlockPos, BlockArray.TileEntityCallback> callbacks = template.getTileCallbacks();
        boolean any = false;
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : template.getPattern().entrySet()) {
            BlockPos at = origin.add(entry.getKey());
            if (!structurebb.isVecInside(at)) continue;
            world.setBlockState(at, entry.getValue().state, 2);
            any = true;
            if (callbacks != null && callbacks.containsKey(entry.getKey())) {
                TileEntity te = world.getTileEntity(at);
                BlockArray.TileEntityCallback callback = callbacks.get(entry.getKey());
                if (callback != null && callback.isApplicable(te)) {
                    callback.onPlace(world, at, te);
                }
            }
        }
        if (any && VillageDebug.once("shrine-piece-" + origin.getX() + "," + origin.getZ())) {
            VillageDebug.log("astral small shrine village piece at=%d,%d,%d", origin.getX(), origin.getY(), origin.getZ());
        }
        return true;
    }

    static StructureBlockArray template() {
        try {
            return MultiBlockArrays.smallShrine;
        } catch (Throwable t) {
            return null;
        }
    }
}
