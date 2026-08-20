package com.apocollis.aqtweaks.mixin.astral;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import hellfirepvp.astralsorcery.common.structure.array.StructureBlockArray;
import hellfirepvp.astralsorcery.common.world.WorldGenAttributeCommon;
import hellfirepvp.astralsorcery.common.world.structure.StructureTreasureShrine;
import hellfirepvp.astralsorcery.common.world.structure.WorldGenAttributeStructure;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(value = WorldGenAttributeCommon.class, remap = false)
public abstract class MixinWorldGenAttributeCommon {

    @Inject(method = "tryGenerateAtPosition", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipVillageOverlap(BlockPos pos, World world, Random random, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableAstralShrineSettle) return;
        if (!StructureVillageOverlap.enabled() || pos == null || world == null) return;
        if (!((Object) this instanceof WorldGenAttributeStructure)) return;
        if ((Object) this instanceof StructureTreasureShrine) return;
        WorldGenAttributeStructure structure = (WorldGenAttributeStructure) (Object) this;
        StructureBlockArray template = structure.getStructureTemplate();
        if (template == null || template.getPattern() == null || template.getPattern().isEmpty()) return;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos offset : template.getPattern().keySet()) {
            BlockPos at = pos.add(offset);
            minX = Math.min(minX, at.getX());
            maxX = Math.max(maxX, at.getX());
            minZ = Math.min(minZ, at.getZ());
            maxZ = Math.max(maxZ, at.getZ());
            minY = Math.min(minY, at.getY());
            maxY = Math.max(maxY, at.getY());
        }
        if (minX > maxX) return;
        if (StructureVillageOverlap.overlapsVillage(world, minX, maxX, minZ, maxZ, minY, maxY)) {
            VillageDebug.log("astral skip village overlap at=%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
            ci.cancel();
        }
    }
}
