package com.apocollis.aqtweaks.rtg;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import java.util.Random;

/**
 * Non-placing village child whose AABB is the flatten plate (pad + Hermite) and
 * well-floor through plate + box height. Saved in {@code Village.dat} with the real pieces.
 */
public class VillagePieceVillagePlate extends StructureVillagePieces.Village {

    public VillagePieceVillagePlate() {}

    public VillagePieceVillagePlate(StructureVillagePieces.Start start, StructureBoundingBox box) {
        super(start, 0);
        this.setCoordBaseMode(EnumFacing.NORTH);
        this.boundingBox = box;
        this.averageGroundLvl = box != null ? box.minY : 0;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox structurebb) {
        return true;
    }
}
