package com.apocollis.aqtweaks.mixin.atum;

import com.apocollis.aqtweaks.atum.AtumPyramidPlate;
import com.teammetallurgy.atum.world.ChunkGeneratorAtum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Random;

/**
 * Moves the pyramid template origin so template Y=8 sits on the averaged footprint height.
 * Atum still rejects a start whose own 5×5 sample is below Y 53.
 */
@Mixin(targets = "com.teammetallurgy.atum.world.gen.structure.pyramid.MapGenPyramid$Start", remap = false)
public class MixinMapGenPyramidStart {

    @Inject(method = "create", at = @At("HEAD"), remap = false)
    private void aqtweaks$preparePyramidPlate(World world, ChunkGeneratorAtum generator, Random random, int chunkX, int chunkZ, CallbackInfo ci) {
        AtumPyramidPlate.prepare(world, generator);
    }

    @ModifyArgs(
            method = "create",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/teammetallurgy/atum/world/gen/structure/pyramid/PyramidPieces;getComponents(Lnet/minecraft/world/gen/structure/template/TemplateManager;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Rotation;Ljava/util/Random;)Ljava/util/List;"
            ),
            remap = false)
    private void aqtweaks$raisePyramidOrigin(Args args) {
        BlockPos origin = args.get(1);
        net.minecraft.util.Rotation rotation = args.get(2);
        args.set(1, AtumPyramidPlate.retarget(origin, rotation));
    }

    @Inject(method = "create", at = @At("RETURN"), remap = false)
    private void aqtweaks$clearPyramidPlate(World world, ChunkGeneratorAtum generator, Random random, int chunkX, int chunkZ, CallbackInfo ci) {
        AtumPyramidPlate.clearPrepare();
    }
}
