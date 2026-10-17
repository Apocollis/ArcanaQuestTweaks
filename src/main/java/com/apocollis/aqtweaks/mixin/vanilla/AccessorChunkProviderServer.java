package com.apocollis.aqtweaks.mixin.vanilla;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkProviderServer.class)
public interface AccessorChunkProviderServer {

    @Accessor("chunkGenerator")
    IChunkGenerator aqtweaks$getChunkGenerator();

    @Accessor("loadedChunks")
    Long2ObjectMap<Chunk> aqtweaks$getLoadedChunks();
}
