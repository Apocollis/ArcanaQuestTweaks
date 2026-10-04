package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.World;
import net.minecraft.world.gen.MapGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Random;

/** Protected {@code MapGenBase} state used by the village and mineshaft code. */
@Mixin(MapGenBase.class)
public interface AccessorMapGenBase {

    @Accessor("world")
    World aqtweaks$getWorld();

    @Accessor("world")
    void aqtweaks$setWorld(World world);

    @Accessor("rand")
    Random aqtweaks$getRand();
}
