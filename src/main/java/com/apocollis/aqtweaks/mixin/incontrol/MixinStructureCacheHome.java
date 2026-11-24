package com.apocollis.aqtweaks.mixin.incontrol;

import com.apocollis.aqtweaks.comfort.home.HomeNames;
import com.apocollis.aqtweaks.comfort.home.HomeRegistry;
import mcjty.tools.cache.StructureCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * In Control's {@code structure} rule does not use vanilla's structure test: {@code StructureCache.isInStructure}
 * reads the saved structure data of the world. A registered player home is not in that data, so the name
 * {@code home} is answered from the home registry here. Every other name is left to the stock lookup.
 */
@Mixin(value = StructureCache.class, remap = false)
public abstract class MixinStructureCacheHome {

    @Inject(method = "isInStructure", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$homeIsAStructure(World world, String structure, BlockPos pos,
            CallbackInfoReturnable<Boolean> cir) {
        if (HomeNames.isHomeName(structure)) {
            cir.setReturnValue(HomeRegistry.structureAt(world, pos) != null);
        }
    }
}
