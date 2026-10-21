package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.bettermineshafts.MineshaftSurfaceShaft;
import com.apocollis.aqtweaks.rtg.StructureAccess;
import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * RTG 9.0.3 pastes mineshafts through its own {@code ChunkGeneratorRTG.generateStructure} instead
 * of calling {@link StructureStart#generateStructure} ({@code func_75068_a}), so
 * {@link MixinStructureStartMineshaftBox} never runs on RTG chunks. This hook mirrors that logic
 * at the RETURN of RTG's custom structure paste method, refreshing the Start's bounding box and
 * carving the surface shaft fallback when guarded to Better Mineshafts Starts.
 */
@Mixin(targets = "rtg.world.gen.ChunkGeneratorRTG", remap = false)
public abstract class MixinChunkGeneratorRTGMineshaftPaste {

    @Inject(method = "generateStructure(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/world/gen/structure/StructureBoundingBox;Lnet/minecraft/world/gen/structure/StructureStart;)V", at = @At("RETURN"))
    private void aqtweaks$refreshRtgMineshaftBox(World world, Random rand, StructureBoundingBox box, StructureStart start, CallbackInfo ci) {
        if (!(start instanceof MapGenBetterMineshaft.Start)) return;
        StructureAccess.updateStructureStartBoundingBox(start);
        if (ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.surfaceShaftFallback) {
            MineshaftSurfaceShaft.carveForStart(world, start, box);
        }
    }
}
