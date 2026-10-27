package com.apocollis.aqtweaks.mixin.lighting;

import com.apocollis.aqtweaks.world.NonRtgLightFixTrigger;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link NonRtgLightFixTrigger} that a chunk has finished populating, whichever generator populated it.
 * Early json: {@link Chunk} is already loaded when late mixins prepare. This class imports no parent mod types.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkPopulateLight {

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private World world;

    @Inject(method = "populate(Lnet/minecraft/world/gen/IChunkGenerator;)V", at = @At("RETURN"))
    private void aqtweaks$chunkPopulated(IChunkGenerator generator, CallbackInfo ci) {
        NonRtgLightFixTrigger.onPopulated(this.world, (Chunk) (Object) this);
    }
}
