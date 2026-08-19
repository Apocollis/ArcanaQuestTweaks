package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.rtg.VillagePlate;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MapGenVillage.class, remap = false)
public abstract class MixinMapGenVillageStart {

    @Inject(method = "func_75049_b", at = @At("RETURN"))
    private void aqtweaks$rememberVillageStart(int chunkX, int chunkZ, CallbackInfoReturnable<StructureStart> cir) {
        StructureStart start = cir.getReturnValue();
        if (start == null) return;
        World world = Reflect.getMapGenWorld(this);
        VillagePlate.remember(world, start);
    }
}
