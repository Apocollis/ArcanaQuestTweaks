package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.comfort.home.HomeNames;
import com.apocollis.aqtweaks.comfort.home.HomeRegistry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's structure test ({@code isInsideStructure(world, name, pos)}) is what Music Triggers and
 * structure-based spawn rules ask. A registered player home answers true for the structure name {@code Home},
 * whatever chunk generator the world uses. Other names fall through to vanilla untouched.
 */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerHome {

    @Inject(method = "isInsideStructure", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$homeIsAStructure(World world, String structureName, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (HomeNames.isHomeName(structureName)) {
            cir.setReturnValue(HomeRegistry.structureAt(world, pos) != null);
        }
    }
}
