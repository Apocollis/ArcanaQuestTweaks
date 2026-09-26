package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Chunk.class)
public interface AccessorChunk {

    @Accessor("queuedLightChecks")
    int getQueuedLightChecks();
}
