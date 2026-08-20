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
    private boolean aqtweaks$skipHut;

    @Inject(method = "generate", at = @At("HEAD"))
    private void aqtweaks$resetHutSkip(Random random, int chunkX, int chunkZ, World world,
                                      IChunkGenerator chunkGenerator, IChunkProvider chunkProvider,
                                      CallbackInfo ci) {
        aqtweaks$skipHut = false;
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
        aqtweaks$skipHut = false;
        if (aqtweaks$isHut()
                && ArcanaQuestTweaksConfig.RtgModuleConfig.surface.enableMysticalHutSettle
                && StructureVillageOverlap.enabled()
                && StructureVillageOverlap.overlapsVillage(world, pos, template.getSize(), settings)) {
            aqtweaks$skipHut = true;
            VillageDebug.log("mystical hut skip village overlap at=%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
            return;
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
        if (aqtweaks$skipHut) {
            return java.util.Collections.emptyMap();
        }
        return template.getDataBlocks(pos, settings);
    }

    @Redirect(
            method = "generate",
            at = @At(
                    value = "INVOKE",
                    target = "Lepicsquid/mysticalworld/world/data/DataHelper;putBlockPos(Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/World;)V"
            )
    )
    private void aqtweaks$skipHutMark(ResourceLocation id, BlockPos pos, World world) {
        if (aqtweaks$skipHut) return;
        DataHelper.putBlockPos(id, pos, world);
    }

    @Unique
    private boolean aqtweaks$isHut() {
        if (structure == null) return false;
        String path = structure.getPath();
        return path != null && path.toLowerCase().contains("hut");
    }
}
