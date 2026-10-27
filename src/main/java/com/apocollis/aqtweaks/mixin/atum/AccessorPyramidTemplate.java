package com.apocollis.aqtweaks.mixin.atum;

import net.minecraft.util.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Entrance facing of a placed Atum pyramid. The field lives on Atum's template component. */
@Mixin(targets = "com.teammetallurgy.atum.world.gen.structure.pyramid.PyramidPieces$PyramidTemplate", remap = false)
public interface AccessorPyramidTemplate {

    @Accessor("rotation")
    Rotation aqtweaks$rotation();
}
