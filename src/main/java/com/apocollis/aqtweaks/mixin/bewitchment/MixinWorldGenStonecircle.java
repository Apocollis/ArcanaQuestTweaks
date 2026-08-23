package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.bewitchment.common.world.gen.structures.WorldGenStonecircle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldGenStonecircle.class, remap = false)
public abstract class MixinWorldGenStonecircle {

    @Redirect(
            method = "func_180709_b",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/structure/template/Template;func_186253_b(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/PlacementSettings;)V"
            )
    )
    private void aqtweaks$placeUnlessVillage(Template template, World world, BlockPos pos,
                                            PlacementSettings settings) {
        BlockPos at = StructureVillageOverlap.relocateOrSkip(world, template, pos, settings, "stonecircle");
        if (at == null || template == null) return;
        template.addBlocksToWorld(world, at, settings);
    }
}
