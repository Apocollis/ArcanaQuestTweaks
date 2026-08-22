package com.apocollis.aqtweaks.mixin.astral;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureLandSettle;
import hellfirepvp.astralsorcery.common.block.BlockMarble;
import hellfirepvp.astralsorcery.common.structure.array.StructureBlockArray;
import hellfirepvp.astralsorcery.common.world.structure.StructureSmallRuin;
import hellfirepvp.astralsorcery.common.world.structure.StructureSmallShrine;
import hellfirepvp.astralsorcery.common.world.structure.StructureTreasureShrine;
import hellfirepvp.astralsorcery.common.world.structure.WorldGenAttributeStructure;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = WorldGenAttributeStructure.class, remap = false)
public abstract class MixinWorldGenAttributeStructure {

    @Inject(method = "generateAsSubmergedStructure", at = @At("RETURN"))
    private void aqtweaks$settleShrineLand(World world, BlockPos center, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableAstralShrineSettle) return;
        if (!StructureLandSettle.enabled() || world == null || center == null) return;
        WorldGenAttributeStructure self = (WorldGenAttributeStructure) (Object) this;
        if (self instanceof StructureTreasureShrine) return;
        StructureBlockArray template = self.getStructureTemplate();
        if (template == null || template.getPattern() == null) return;

        boolean small = self instanceof StructureSmallShrine || self instanceof StructureSmallRuin;
        Map<Long, Integer> floors = new HashMap<>();
        int walkway = center.getY();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos offset : template.getPattern().keySet()) {
            BlockPos at = center.add(offset);
            minX = Math.min(minX, at.getX());
            maxX = Math.max(maxX, at.getX());
            minZ = Math.min(minZ, at.getZ());
            maxZ = Math.max(maxZ, at.getZ());
            minY = Math.min(minY, at.getY());
            maxY = Math.max(maxY, at.getY());
            long key = StructureLandSettle.pack(at.getX(), at.getZ());
            if (small) {
                floors.put(key, walkway);
            } else {
                floors.merge(key, at.getY(), Integer::min);
            }
        }
        if (minX > maxX) return;
        int bank = small
                ? Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.smallShrinePad)
                : Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.structureRimBank);
        IBlockState underFill = small ? null : BlockMarble.MarbleBlockType.RAW.asBlock();
        StructureLandSettle.settle(world, floors, small, bank, underFill);
        if (!small) {
            StructureLandSettle.clearFoliage(world, minX, maxX, minZ, maxZ, minY, maxY + 16);
        }
    }
}
