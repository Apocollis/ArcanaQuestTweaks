package com.apocollis.aqtweaks.nether;

import com.apocollis.aqtweaks.mixin.vanilla.AccessorChunkProviderServer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Reads blocks from chunks that are already loaded, without clearing the chunk's unload flag (which
 * {@code World.getBlockState} and {@code getLoadedChunk} do) and without ever loading or generating a chunk.
 * Returns null for an unloaded chunk or a height out of range.
 */
public final class LoadedBlocks {
    private LoadedBlocks() {}

    public static IBlockState peek(World world, int x, int y, int z) {
        if (y < 0 || y > 255) return null;
        if (world.isRemote) {
            BlockPos pos = new BlockPos(x, y, z);
            return world.isBlockLoaded(pos) ? world.getBlockState(pos) : null;
        }
        IChunkProvider provider = world.getChunkProvider();
        if (provider instanceof AccessorChunkProviderServer accessor) {
            Chunk chunk = accessor.aqtweaks$getLoadedChunks().get(ChunkPos.asLong(x >> 4, z >> 4));
            return chunk == null ? null : chunk.getBlockState(x, y, z);
        }
        return null;
    }
}
