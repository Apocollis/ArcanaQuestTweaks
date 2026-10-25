package com.apocollis.aqtweaks.mixin.lighting;

import com.apocollis.aqtweaks.world.BulkTemplateRelight;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Brackets a Template paste for {@link BulkTemplateRelight} and times it for {@code [AQ-PERF]}. */
@Mixin(Template.class)
public abstract class MixinTemplateBulkRelight {

    @Inject(method = "addBlocksToWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/ITemplateProcessor;Lnet/minecraft/world/gen/structure/template/PlacementSettings;I)V",
            at = @At("HEAD"))
    private void aqtweaks$pasteHead(World world, BlockPos pos, @Nullable ITemplateProcessor processor,
                                    PlacementSettings settings, int flags, CallbackInfo ci) {
        BlockPos size = ((Template) (Object) this).getSize();
        BulkTemplateRelight.begin(world, size.getX(), size.getY(), size.getZ());
    }

    @Inject(method = "addBlocksToWorld(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/structure/template/ITemplateProcessor;Lnet/minecraft/world/gen/structure/template/PlacementSettings;I)V",
            at = @At("RETURN"))
    private void aqtweaks$pasteReturn(World world, BlockPos pos, @Nullable ITemplateProcessor processor,
                                      PlacementSettings settings, int flags, CallbackInfo ci) {
        BulkTemplateRelight.end();
    }
}
