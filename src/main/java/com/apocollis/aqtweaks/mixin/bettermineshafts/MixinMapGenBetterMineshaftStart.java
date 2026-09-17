package com.apocollis.aqtweaks.mixin.bettermineshafts;

import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettermineshafts.world.MapGenBetterMineshaft;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(value = MapGenBetterMineshaft.Start.class, remap = false)
public abstract class MixinMapGenBetterMineshaftStart extends StructureStart {

    @Inject(method = "func_75068_a", at = @At("RETURN"))
    private void aqtweaks$refreshMineshaftBox(World world, Random rand, StructureBoundingBox box, CallbackInfo ci) {
        Reflect.updateStructureStartBoundingBox(this);
    }
}
