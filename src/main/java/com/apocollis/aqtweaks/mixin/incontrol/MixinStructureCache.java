package com.apocollis.aqtweaks.mixin.incontrol;

import com.apocollis.aqtweaks.spawning.StructureCacheHooks;
import java.util.Set;
import mcjty.tools.cache.StructureCache;
import net.minecraft.world.gen.structure.MapGenStructureData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StructureCache.class, remap = false)
public abstract class MixinStructureCache {

    @Inject(method = "parseStructureData", at = @At("RETURN"))
    private static void aqtweaks$expandBoundingBoxChunks(
            MapGenStructureData data, CallbackInfoReturnable<Set<Long>> cir) {
        StructureCacheHooks.addBoundingBoxChunks(data, cir.getReturnValue());
    }
}
