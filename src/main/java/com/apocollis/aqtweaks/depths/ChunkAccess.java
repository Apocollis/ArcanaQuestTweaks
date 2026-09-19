package com.apocollis.aqtweaks.depths;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Direct chunk/world access for the Depths Y≥0 seam pass.
 *
 * <p>{@code MixinChunkProviderServer} is {@code remap = false}, so vanilla get/set must not be
 * written in that mixin. Null and throw behaviour matches {@link com.apocollis.aqtweaks.util.Reflect}.
 */
public final class ChunkAccess {

    private static final ThreadLocal<BlockPos.MutableBlockPos> POS =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    private static IBlockState airState;

    private ChunkAccess() {}

    private static IBlockState air() {
        IBlockState state = airState;
        if (state == null) {
            state = Blocks.AIR.getDefaultState();
            airState = state;
        }
        return state;
    }

    public static long getSeed(World world) {
        if (world == null) return 0L;
        try {
            return world.getSeed();
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static IBlockState getBlockState(Chunk chunk, int x, int y, int z) {
        if (chunk == null) return air();
        try {
            IBlockState state = chunk.getBlockState(POS.get().setPos(x, y, z));
            return state != null ? state : air();
        } catch (Throwable t) {
            return air();
        }
    }

    public static IBlockState setBlockState(Chunk chunk, int x, int y, int z, IBlockState state) {
        if (chunk == null || state == null) return null;
        try {
            return chunk.setBlockState(POS.get().setPos(x, y, z), state);
        } catch (Throwable t) {
            return null;
        }
    }
}
