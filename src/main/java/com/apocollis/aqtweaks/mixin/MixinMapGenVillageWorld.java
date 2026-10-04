package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.StructureVillageOverlap;
import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtg.world.gen.ChunkGeneratorRTG;

@Mixin(value = MapGenBase.class, remap = false)
public abstract class MixinMapGenVillageWorld {

    /**
     * One entry per live {@code generate} call on this thread: whether that call pushed a generator.
     * {@code VillageLandHelper} re-enters {@code generate} for neighbouring well chunks while laying
     * out, so a single per-instance flag would be clobbered by the nested call and the outer call
     * would never pop its generator.
     */
    @Unique
    private static final ThreadLocal<java.util.ArrayDeque<Boolean>> AQTWEAKS$PUSHED =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);

    /**
     * {@code MapGenBase.generate(World, int, int, ChunkPrimer)} is {@code func_186125_a} in 1.12.2.
     * ({@code func_151539_a} is the 1.7/1.8 name and does not exist here.)
     */
    @Inject(method = "func_186125_a", at = @At("HEAD"))
    private void aqtweaks$pushVillageWorld(World world, int x, int z, ChunkPrimer primer, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenVillage)) return;
        VillageLandHelper.pushWorld(world);
        if (VillageLandHelper.currentGenerator() != null) {
            AQTWEAKS$PUSHED.get().push(Boolean.FALSE);
            VillageLandHelper.stashGenerators(world, (MapGenVillage) (Object) this, VillageLandHelper.currentGenerator());
            return;
        }
        boolean pushed = false;
        ChunkGeneratorRTG found = StructureVillageOverlap.findRtgGenerator(world);
        if (found != null) {
            VillageLandHelper.pushGenerator(found);
            pushed = true;
        }
        AQTWEAKS$PUSHED.get().push(pushed);
        VillageLandHelper.stashGenerators(world, (MapGenVillage) (Object) this, VillageLandHelper.currentGenerator());
    }

    @Inject(method = "func_186125_a", at = @At("RETURN"))
    private void aqtweaks$popVillageWorld(World world, int x, int z, ChunkPrimer primer, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenVillage)) return;
        try {
            if (!VillageLandHelper.isSamplingLandscape() && !VillageLandHelper.isLayingOut()) {
                VillageLandHelper.forgetRejectedStarts((MapGenVillage) (Object) this, world);
            }
        } finally {
            java.util.ArrayDeque<Boolean> pushed = AQTWEAKS$PUSHED.get();
            if (!pushed.isEmpty() && Boolean.TRUE.equals(pushed.pop())) {
                VillageLandHelper.popGenerator();
            }
            VillageLandHelper.popWorld();
        }
    }
}
