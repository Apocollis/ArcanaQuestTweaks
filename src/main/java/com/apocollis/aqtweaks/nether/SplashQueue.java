package com.apocollis.aqtweaks.nether;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/**
 * The thrown-lava queue: every cell a spout placed, with its expiry tick. This is the one piece of world change
 * the Nether hazards persist (a position list), so a restart does not leave stationary lava behind. A tracked
 * cell is cleared only if it is still lava with {@code LEVEL > 0}; a source is never touched.
 */
public class SplashQueue extends WorldSavedData {
    public static final String ID = "aqtweaks_splash";

    private final Long2LongOpenHashMap cells = new Long2LongOpenHashMap();

    public SplashQueue() {
        super(ID);
    }

    public SplashQueue(String name) {
        super(name);
    }

    public static SplashQueue get(World world) {
        SplashQueue data = (SplashQueue) world.getPerWorldStorage().getOrLoadData(SplashQueue.class, ID);
        if (data == null) {
            data = new SplashQueue();
            world.getPerWorldStorage().setData(ID, data);
        }
        return data;
    }

    public int size() {
        return cells.size();
    }

    public void add(BlockPos pos, long expireTick) {
        cells.put(pos.toLong(), expireTick);
        markDirty();
    }

    /** Clears expired cells whose chunk is loaded. Unloaded ones wait until the chunk loads. */
    public void sweep(World world, long now) {
        if (cells.isEmpty()) return;
        LongIterator it = cells.keySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            long packed = it.nextLong();
            if (cells.get(packed) > now) continue;
            BlockPos pos = BlockPos.fromLong(packed);
            IBlockState state = LoadedBlocks.peek(world, pos.getX(), pos.getY(), pos.getZ());
            if (state == null) continue;
            wipe(world, pos, state);
            it.remove();
            changed = true;
        }
        if (changed) markDirty();
    }

    /** Clears the cells of a chunk that is about to unload (it is still in memory). */
    public void clearChunk(World world, int chunkX, int chunkZ) {
        if (cells.isEmpty()) return;
        LongIterator it = cells.keySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            BlockPos pos = BlockPos.fromLong(it.nextLong());
            if ((pos.getX() >> 4) != chunkX || (pos.getZ() >> 4) != chunkZ) continue;
            IBlockState state = LoadedBlocks.peek(world, pos.getX(), pos.getY(), pos.getZ());
            if (state != null) wipe(world, pos, state);
            it.remove();
            changed = true;
        }
        if (changed) markDirty();
    }

    /** Clears every tracked cell that can be reached now and forgets the rest. */
    public void clearAll(World world) {
        LongIterator it = cells.keySet().iterator();
        while (it.hasNext()) {
            BlockPos pos = BlockPos.fromLong(it.nextLong());
            IBlockState state = LoadedBlocks.peek(world, pos.getX(), pos.getY(), pos.getZ());
            if (state != null) wipe(world, pos, state);
        }
        cells.clear();
        markDirty();
    }

    private static void wipe(World world, BlockPos pos, IBlockState state) {
        if ((state.getBlock() == Blocks.FLOWING_LAVA || state.getBlock() == Blocks.LAVA)
                && state.getValue(BlockLiquid.LEVEL) > 0) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        cells.clear();
        NBTTagList list = nbt.getTagList("cells", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            cells.put(tag.getLong("p"), tag.getLong("e"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        LongIterator it = cells.keySet().iterator();
        while (it.hasNext()) {
            long p = it.nextLong();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("p", p);
            tag.setLong("e", cells.get(p));
            list.appendTag(tag);
        }
        compound.setTag("cells", list);
        return compound;
    }
}
