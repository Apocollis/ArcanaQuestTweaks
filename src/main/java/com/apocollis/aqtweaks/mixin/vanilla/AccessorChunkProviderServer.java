package com.apocollis.aqtweaks.mixin.vanilla;

import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkProviderServer.class)
public interface AccessorChunkProviderServer {

    @Accessor("chunkGenerator")
    IChunkGenerator aqtweaks$getChunkGenerator();
}
