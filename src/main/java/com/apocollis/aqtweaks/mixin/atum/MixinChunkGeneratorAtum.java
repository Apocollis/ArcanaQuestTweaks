package com.apocollis.aqtweaks.mixin.atum;

import com.apocollis.aqtweaks.atum.AtumPyramidSmoothing;
import com.teammetallurgy.atum.world.gen.structure.pyramid.MapGenPyramid;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Smooths the primer after the pyramid start is created and before this chunk is built.
 * Structure blocks are pasted later, during populate, on top of this terrain.
 */
@Mixin(targets = "com.teammetallurgy.atum.world.ChunkGeneratorAtum", remap = false)
public class MixinChunkGeneratorAtum {

    @Redirect(
            method = "func_185932_a",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/teammetallurgy/atum/world/gen/structure/pyramid/MapGenPyramid;func_186125_a(Lnet/minecraft/world/World;IILnet/minecraft/world/chunk/ChunkPrimer;)V"
            ),
            remap = false)
    private void aqtweaks$smoothPyramidTerrain(MapGenPyramid pyramid, World world, int chunkX, int chunkZ, ChunkPrimer primer) {
        pyramid.generate(world, chunkX, chunkZ, primer);
        AtumPyramidSmoothing.apply(world, primer, chunkX, chunkZ, pyramid);
    }
}
