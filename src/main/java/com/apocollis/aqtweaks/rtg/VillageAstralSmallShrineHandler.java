package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.List;
import java.util.Random;

public final class VillageAstralSmallShrineHandler implements VillagerRegistry.IVillageCreationHandler {

    private VillageAstralSmallShrineHandler() {}

    public static void register() {
        MapGenStructureIO.registerStructureComponent(VillagePieceAstralSmallShrine.class, "AQTSmallShrine");
        VillagerRegistry.instance().registerVillageCreationHandler(new VillageAstralSmallShrineHandler());
    }

    @Override
    public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int villageSize) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableAstralSmallShrineVillagePiece) {
            return null;
        }
        return new StructureVillagePieces.PieceWeight(VillagePieceAstralSmallShrine.class, 5, 1);
    }

    @Override
    public Class<?> getComponentClass() {
        return VillagePieceAstralSmallShrine.class;
    }

    @Override
    public StructureVillagePieces.Village buildComponent(StructureVillagePieces.PieceWeight villagePiece,
                                                         StructureVillagePieces.Start startPiece,
                                                         List<StructureComponent> pieces, Random random,
                                                         int x, int y, int z, EnumFacing facing, int type) {
        VillagePieceAstralSmallShrine placed = VillagePieceAstralSmallShrine.build(
                startPiece, pieces, random, x, y, z, facing, type);
        if (placed == null) return null;
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.skipWaterVillagePieces) return placed;
        if (!VillageLandHelper.isAabbWet(startPiece, placed)) return placed;

        VillageLandHelper.removeVillagePiece(startPiece, pieces, placed);
        VillageDebug.log("astral shrine aabb wet origin=%d,%d, retrying inland", x, z);
        int maxStep = Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.villageWaterRetryDistance);
        for (int[] slot : VillageLandHelper.inlandCandidates(startPiece, x, z, facing, maxStep)) {
            VillagePieceAstralSmallShrine retry = VillagePieceAstralSmallShrine.build(
                    startPiece, pieces, random, slot[0], y, slot[1], facing, type);
            if (retry != null && !VillageLandHelper.isAabbWet(startPiece, retry)) {
                VillageDebug.log("astral shrine retry hit origin=%d,%d slot=%d,%d", x, z, slot[0], slot[1]);
                return retry;
            }
            if (retry != null) {
                VillageLandHelper.removeVillagePiece(startPiece, pieces, retry);
            }
        }
        VillageDebug.log("astral shrine retry miss origin=%d,%d", x, z);
        return null;
    }
}
