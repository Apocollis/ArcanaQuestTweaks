package com.apocollis.aqtweaks.mixin.lighting;

import com.apocollis.aqtweaks.world.BulkTemplateRelight;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records {@code Chunk.relightBlock} while a big Template paste runs ({@link BulkTemplateRelight}).
 * Priority 500 so this HEAD hook runs before Depths Update's own {@code relightBlock} handler (priority
 * 1000), which cancels vanilla for extended dimensions. The replay goes back through both.
 * Early json: {@link Chunk} is already loaded when late mixins prepare.
 */
@Mixin(value = Chunk.class, priority = 500)
public abstract class MixinChunkBulkRelight implements BulkTemplateRelight.RelightAccess {

    @Shadow
    private void relightBlock(int x, int y, int z) {
    }

    @Inject(method = "relightBlock(III)V", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$bulkRelight(int x, int y, int z, CallbackInfo ci) {
        if (BulkTemplateRelight.onRelight((Chunk) (Object) this, x, y, z)) {
            ci.cancel();
        }
    }

    @Override
    public void aqtweaks$relightBlock(int x, int y, int z) {
        this.relightBlock(x, y, z);
    }
}
