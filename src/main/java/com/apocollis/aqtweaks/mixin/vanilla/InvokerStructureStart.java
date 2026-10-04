package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * {@code StructureStart.updateBoundingBox} ({@code func_75072_c}) is protected, so the reflective
 * {@code getMethod} lookup in {@code Reflect} never resolved it and village pad insertion left the
 * start's bounding box stale. Used by {@code StructureAccess.updateStructureStartBoundingBox}.
 */
@Mixin(StructureStart.class)
public interface InvokerStructureStart {

    @Invoker("updateBoundingBox")
    void aqtweaks$updateBoundingBox();
}
