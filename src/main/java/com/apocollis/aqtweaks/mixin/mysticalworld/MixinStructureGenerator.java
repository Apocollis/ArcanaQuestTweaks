package com.apocollis.aqtweaks.mixin.mysticalworld;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.rtg.StructureLandSettle;
import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageDebug;
import epicsquid.mysticalworld.world.StructureGenerator;
import epicsquid.mysticalworld.world.data.DataHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(value = StructureGenerator.class, remap = false)
public abstract class MixinStructureGenerator {

    @Shadow
    private ResourceLocation structure;

    @Unique
    private boolean aqtweaks$skipStructure;

    @Unique
    private BlockPos aqtweaks$pastePos;

    @Inject(method = "generate", at = @At("HEAD"))
    private void aqtweaks$resetHutSkip(Random random, int chunkX, int chunkZ, World world,
                                      IChunkGenerator chunkGenerator, IChunkProvider chunkProvider,
                                      CallbackInfo ci) {
        aqtweaks$skipStructure = false;
        aqtweaks$pastePos = null;
    }

    @Redirect(
            method = "generate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/template/Template;func_189962_a(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/PlacementSettings;I)V"
            )
    )
    private void aqtweaks$placeHutUnlessVillage(Template template, World world, BlockPos pos,
                                               PlacementSettings settings, int flags) {
        aqtweaks$skipStructure = false;
        aqtweaks$pastePos = pos;
        if (aqtweaks$isMwSurface()
                && StructureVillageOverlap.enabled()
                && aqtweaks$surfaceSkipEnabled()
                && StructureVillageOverlap.overlapsVillage(world, pos, template.getSize(), settings)) {
            BlockPos retry = StructureVillageOverlap.findNearbyLand(world, template, pos, settings);
            if (retry == null) {
                aqtweaks$skipStructure = true;
                VillageDebug.log("mystical %s skip village overlap at=%d,%d,%d",
                        aqtweaks$structureName(), pos.getX(), pos.getY(), pos.getZ());
                return;
            }
            VillageDebug.log("mystical %s relocate from=%d,%d,%d to=%d,%d,%d",
                    aqtweaks$structureName(), pos.getX(), pos.getY(), pos.getZ(),
                    retry.getX(), retry.getY(), retry.getZ());
            pos = retry;
            aqtweaks$pastePos = retry;
        }
        template.addBlocksToWorld(world, pos, settings, flags);
        if (aqtweaks$isHut() && ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableMysticalHutSettle) {
            StructureLandSettle.settleTemplate(world, pos, template.getSize(), settings);
        }
    }

    @Redirect(
            method = "generate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/template/Template;func_186258_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/PlacementSettings;)Ljava/util/Map;"
            )
    )
    private java.util.Map aqtweaks$skipHutData(Template template, BlockPos pos, PlacementSettings settings) {
        if (aqtweaks$skipStructure) {
            return java.util.Collections.emptyMap();
        }
        BlockPos at = aqtweaks$pastePos != null ? aqtweaks$pastePos : pos;
        return template.getDataBlocks(at, settings);
    }

    @Redirect(
            method = "generate",
            at = @At(
                    value = "INVOKE",
                    target = "Lepicsquid/mysticalworld/world/data/DataHelper;putBlockPos(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;)V"
            )
    )
    private void aqtweaks$skipHutMark(ResourceLocation id, BlockPos pos, World world) {
        if (aqtweaks$skipStructure) return;
        DataHelper.putBlockPos(id, aqtweaks$pastePos != null ? aqtweaks$pastePos : pos, world);
    }

    @Unique
    private boolean aqtweaks$surfaceSkipEnabled() {
        if (aqtweaks$isHut()) {
            return ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableMysticalHutSettle;
        }
        return aqtweaks$isBarrow();
    }

    @Unique
    private boolean aqtweaks$isMwSurface() {
        return aqtweaks$isHut() || aqtweaks$isBarrow();
    }

    @Unique
    private boolean aqtweaks$isHut() {
        return "hut".equals(aqtweaks$structureName());
    }

    @Unique
    private boolean aqtweaks$isBarrow() {
        return "barrow".equals(aqtweaks$structureName());
    }

    @Unique
    private String aqtweaks$structureName() {
        if (structure == null) return "";
        String path = structure.getPath();
        return path == null ? "" : path.toLowerCase();
    }

}
