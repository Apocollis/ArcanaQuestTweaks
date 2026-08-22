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

    @Unique
    private boolean aqtweaks$pushedGenerator;

    @Inject(method = "func_151539_a", at = @At("HEAD"))
    private void aqtweaks$pushVillageWorld(World world, int x, int z, ChunkPrimer primer, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenVillage)) return;
        VillageLandHelper.pushWorld(world);
        aqtweaks$pushedGenerator = false;
        if (VillageLandHelper.currentGenerator() != null) {
            VillageLandHelper.stashGenerators(world, (MapGenVillage) (Object) this, VillageLandHelper.currentGenerator());
            return;
        }
        ChunkGeneratorRTG found = StructureVillageOverlap.findRtgGenerator(world);
        if (found != null) {
            VillageLandHelper.pushGenerator(found);
            aqtweaks$pushedGenerator = true;
        }
        VillageLandHelper.stashGenerators(world, (MapGenVillage) (Object) this, VillageLandHelper.currentGenerator());
    }

    @Inject(method = "func_151539_a", at = @At("RETURN"))
    private void aqtweaks$popVillageWorld(World world, int x, int z, ChunkPrimer primer, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenVillage)) return;
        try {
            if (!VillageLandHelper.isSamplingLandscape()) {
                VillageLandHelper.forgetRejectedStarts((MapGenVillage) (Object) this, world);
            }
        } finally {
            if (aqtweaks$pushedGenerator) {
                VillageLandHelper.popGenerator();
                aqtweaks$pushedGenerator = false;
            }
            VillageLandHelper.popWorld();
        }
    }
}
