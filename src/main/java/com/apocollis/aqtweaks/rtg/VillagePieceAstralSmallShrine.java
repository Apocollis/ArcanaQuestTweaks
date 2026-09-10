package com.apocollis.aqtweaks.rtg;

import hellfirepvp.astralsorcery.common.lib.MultiBlockArrays;
import hellfirepvp.astralsorcery.common.structure.array.BlockArray;
import hellfirepvp.astralsorcery.common.structure.array.StructureBlockArray;
import net.minecraft.block.state.IBlockState;
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
 * Template is pasted unrotated; the AABB is sized from the pattern so every chunk that
 * contains marble also intersects the piece.
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
        if (template == null || template.getPattern() == null) return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos key : template.getPattern().keySet()) {
            if (key == null) continue;
            minX = Math.min(minX, key.getX());
            maxX = Math.max(maxX, key.getX());
            minY = Math.min(minY, key.getY());
            maxY = Math.max(maxY, key.getY());
            minZ = Math.min(minZ, key.getZ());
            maxZ = Math.max(maxZ, key.getZ());
        }
        if (minX > maxX || minY > maxY || minZ > maxZ) return null;
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        StructureBoundingBox box = StructureBoundingBox.getComponentToAddBoundingBox(
                x, y, z, 0, 0, 0, sizeX, sizeY, sizeZ, EnumFacing.SOUTH);
        if (!canVillageGoDeeper(box)) return null;
        StructureComponent hit = StructureComponent.findIntersecting(pieces, box);
        if (hit != null && !VillageLandHelper.isVillageRoad(hit)) return null;
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
        List<BlockPos> fluids = new java.util.ArrayList<>();
        for (Map.Entry<BlockPos, BlockArray.BlockInformation> entry : template.getPattern().entrySet()) {
            BlockPos at = origin.add(entry.getKey());
            if (!structurebb.isVecInside(at)) continue;
            if (VillageLandHelper.isNeverRaiseBiome(world.getBiome(at))) continue;
            IBlockState place = entry.getValue().state;
            boolean liquid = place != null && place.getMaterial().isLiquid();
            world.setBlockState(at, place, liquid ? 3 : 2);
            if (liquid) fluids.add(at);
            any = true;
            if (callbacks != null && callbacks.containsKey(entry.getKey())) {
                TileEntity te = world.getTileEntity(at);
                BlockArray.TileEntityCallback callback = callbacks.get(entry.getKey());
                if (callback != null && callback.isApplicable(te)) {
                    callback.onPlace(world, at, te);
                }
            }
        }
        for (BlockPos at : fluids) {
            IBlockState state = world.getBlockState(at);
            if (state != null && state.getMaterial().isLiquid()) {
                world.neighborChanged(at, state.getBlock(), at);
            }
        }
        if (any) {
            StructureLandSettle.fillLiquidAt(world,
                    this.boundingBox.minX, this.boundingBox.maxX,
                    this.boundingBox.minZ, this.boundingBox.maxZ,
                    this.boundingBox.minY);
            if (VillageDebug.once("shrine-piece-" + origin.getX() + "," + origin.getZ())) {
                VillageDebug.log("astral small shrine village piece at=%d,%d,%d", origin.getX(), origin.getY(), origin.getZ());
            }
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
