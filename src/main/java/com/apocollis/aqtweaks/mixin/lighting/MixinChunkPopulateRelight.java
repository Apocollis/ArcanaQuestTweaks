package com.apocollis.aqtweaks.mixin.lighting;

import com.apocollis.aqtweaks.world.PopulateRelight;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scope and holds for {@link PopulateRelight}: begin and end around {@code populate(IChunkGenerator)}, the opening
 * {@code checkLight()} skipped for a populating or held chunk, and the height getters replaying a pending column.
 * Priority 500 so these hooks run before Depths Update's (1000) and Chunk Steward's. Early json: {@link Chunk} is
 * already loaded when late mixins prepare. Imports no parent mod types.
 */
@Mixin(value = Chunk.class, priority = 500)
public abstract class MixinChunkPopulateRelight implements PopulateRelight.Pending {

    @Shadow
    private boolean isTerrainPopulated;

    @Shadow
    private void relightBlock(int x, int y, int z) {
    }

    @Unique
    private PopulateRelight.Record aqtweaks$pending;

    @Override
    public PopulateRelight.Record aqtweaks$pending() {
        return this.aqtweaks$pending;
    }

    @Override
    public void aqtweaks$setPending(PopulateRelight.Record record) {
        this.aqtweaks$pending = record;
    }

    @Override
    public void aqtweaks$markTerrainPopulated() {
        this.isTerrainPopulated = true;
    }

    @Override
    public void aqtweaks$relightBlockReplay(int x, int y, int z) {
        this.relightBlock(x, y, z);
    }

    /**
     * Begins at the opening {@code checkLight()} call, not at the head: Chunk Steward cancels populate at the head for
     * terrain-only chunks, and a cancel skips every return hook, so a scope begun there would never end. A populate of
     * an already populated chunk only generates structures and never reaches this call.
     */
    @Inject(method = "populate(Lnet/minecraft/world/gen/IChunkGenerator;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;checkLight()V"))
    private void aqtweaks$populateBegin(IChunkGenerator generator, CallbackInfo ci) {
        Chunk self = (Chunk) (Object) this;
        PopulateRelight.begin(self.getWorld(), self);
    }

    @Inject(method = "populate(Lnet/minecraft/world/gen/IChunkGenerator;)V", at = @At("RETURN"))
    private void aqtweaks$populateEnd(IChunkGenerator generator, CallbackInfo ci) {
        PopulateRelight.end((Chunk) (Object) this);
    }

    @Inject(method = "checkLight()V", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$holdCheckLight(CallbackInfo ci) {
        if (PopulateRelight.onCheckLight((Chunk) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "getHeightValue(II)I", at = @At("HEAD"))
    private void aqtweaks$heightRead(int x, int z, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        if (this.aqtweaks$pending != null) {
            PopulateRelight.onHeightRead((Chunk) (Object) this, x, z);
        }
    }

    @Inject(method = "getPrecipitationHeight(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;",
            at = @At("HEAD"))
    private void aqtweaks$precipitationRead(BlockPos pos,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<BlockPos> cir) {
        if (this.aqtweaks$pending != null) {
            PopulateRelight.onHeightRead((Chunk) (Object) this, pos.getX() & 15, pos.getZ() & 15);
        }
    }
}
