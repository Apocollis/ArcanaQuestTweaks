package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.VillageLandHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MapGenBase.class, remap = false)
public abstract class MixinMapGenVillageWorld {

    @Inject(method = "func_151539_a", at = @At("HEAD"))
    private void aqtweaks$pushVillageWorld(World world, int x, int z, ChunkPrimer primer, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenVillage)) return;
        VillageLandHelper.pushWorld(world);
    }

    @Inject(method = "func_151539_a", at = @At("RETURN"))
    private void aqtweaks$popVillageWorld(World world, int x, int z, ChunkPrimer primer, CallbackInfo ci) {
        if (!((Object) this instanceof MapGenVillage)) return;
        VillageLandHelper.popWorld();
    }
}
