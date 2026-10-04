package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@code StructureComponent.boundingBox} is protected and has no setter. */
@Mixin(StructureComponent.class)
public interface AccessorStructureComponent {

    @Accessor("boundingBox")
    void aqtweaks$setBoundingBox(StructureBoundingBox box);
}
