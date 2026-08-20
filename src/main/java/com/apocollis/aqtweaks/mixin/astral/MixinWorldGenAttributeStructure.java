package com.apocollis.aqtweaks.mixin.astral;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureLandSettle;
import hellfirepvp.astralsorcery.common.structure.array.StructureBlockArray;
import hellfirepvp.astralsorcery.common.world.structure.StructureSmallRuin;
import hellfirepvp.astralsorcery.common.world.structure.StructureSmallShrine;
import hellfirepvp.astralsorcery.common.world.structure.WorldGenAttributeStructure;
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
        StructureBlockArray template = self.getStructureTemplate();
        if (template == null || template.getPattern() == null) return;

        boolean small = self instanceof StructureSmallShrine || self instanceof StructureSmallRuin;
        Map<Long, Integer> floors = new HashMap<>();
        int walkway = center.getY();
        for (BlockPos offset : template.getPattern().keySet()) {
            BlockPos at = center.add(offset);
            long key = StructureLandSettle.pack(at.getX(), at.getZ());
            if (small) {
                floors.put(key, walkway);
            } else {
                floors.merge(key, at.getY(), Integer::min);
            }
        }
        int bank = small
                ? Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.smallShrinePad)
                : Math.max(0, ArcanaQuestTweaksConfig.RtgModuleConfig.surface.structureRimBank);
        StructureLandSettle.settle(world, floors, small, bank);
    }
}
