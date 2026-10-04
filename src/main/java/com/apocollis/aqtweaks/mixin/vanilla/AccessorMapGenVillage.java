package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Private village spacing fields. */
@Mixin(MapGenVillage.class)
public interface AccessorMapGenVillage {

    @Accessor("distance")
    int aqtweaks$getDistance();

    @Accessor("minTownSeparation")
    int aqtweaks$getMinTownSeparation();
}
