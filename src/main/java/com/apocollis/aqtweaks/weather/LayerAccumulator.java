package com.apocollis.aqtweaks.weather;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Server side build-up of layered blocks (storm snow, storm sand) across the loaded chunks around players, shared by
 * the Moonfrost snow and the Atum sand. Like vanilla snowfall it samples chunks, but faster, many layers deep and
 * smoothly: a layer meant for a column goes to a lower neighbour when it would leave the column more than the step
 * deeper ({@link LayerLeveling}), and columns in the lee of walls fill slower ({@link LayerShadow}). It only ever
 * places layer blocks into air, keeps no state and never removes anything. Subclasses supply the world, the strength,
 * the settings and the block specifics.
 */
public abstract class LayerAccumulator {

    private final Random rand = new Random();
    private final LayerRate.Carry carry = new LayerRate.Carry();
    private long[] chunkKeys = new long[0];
    private long keysBuiltAt = Long.MIN_VALUE;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final Long2IntOpenHashMap depthCache = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap topCache = new Long2IntOpenHashMap();
    private double windX;
    private double windZ;

    // ---- what the subclass supplies ----

    protected abstract boolean worldMatches(World world);

    protected abstract boolean enabled();

    /** Strength of the storm in this world; below 0.25 nothing is built. */
    protected abstract float strength(WorldServer world);

    protected abstract double ratePerChunkPerSecond();

    protected abstract int maxChangesPerTick();

    protected abstract int radius();

    protected abstract int cap();

    protected abstract int step();

    protected abstract boolean windShadow();

    protected abstract double shadowAngle();

    protected abstract Vec3d wind(WorldServer world);

    protected abstract boolean isLayerBlock(IBlockState state);

    protected abstract int layersOf(IBlockState state);

    protected abstract IBlockState withLayers(IBlockState state, int layers);

    /** A single layer, for a column with none. */
    protected abstract IBlockState firstLayer();

    /** True when a fresh layer can stand at the position (solid top face below, or an 8 layer block). */
    protected abstract boolean canPlaceLayer(WorldServer world, BlockPos pos);

    /**
     * True when the first layer may start in the block at the surface: air by default. A subclass can also accept
     * replaceable ground plants (the layer then simply overwrites them, with no drops).
     */
    protected boolean canStartLayerIn(IBlockState state, WorldServer world, BlockPos pos) {
        return state.getBlock().isAir(state, world, pos);
    }

    /** Column filter at the precipitation height, before the stack is read (biome, base block...). */
    protected boolean columnAllowed(WorldServer world, Chunk chunk, int x, int y, int z) {
        return true;
    }

    // ---- ticking ----

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || !(event.world instanceof WorldServer)) return;
        if (!worldMatches(event.world)) return;
        WorldServer world = (WorldServer) event.world;
        if (!enabled()) {
            carry.reset();
            return;
        }
        float strength = strength(world);
        if (strength < LayerRate.MIN_STRENGTH) {
            carry.reset();
            return;
        }
        long now = world.getTotalWorldTime();
        if (keysBuiltAt == Long.MIN_VALUE || now - keysBuiltAt >= 20L) {
            rebuildChunks(world);
            keysBuiltAt = now;
        }
        if (chunkKeys.length == 0) return;

        int maxChanges = maxChangesPerTick();
        int attempts = carry.take(chunkKeys.length, ratePerChunkPerSecond(), strength);
        attempts = Math.min(attempts, maxChanges * 3);
        if (attempts <= 0) return;

        Vec3d wind = wind(world);
        windX = wind.x;
        windZ = wind.z;
        int changes = 0;
        for (int i = 0; i < attempts && changes < maxChanges; i++) {
            long key = chunkKeys[rand.nextInt(chunkKeys.length)];
            int cx = (int) key;
            int cz = (int) (key >> 32);
            if (world.getChunkProvider().getLoadedChunk(cx, cz) == null) continue;
            int x = (cx << 4) + rand.nextInt(16);
            int z = (cz << 4) + rand.nextInt(16);
            if (placeOne(world, x, z)) changes++;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote || !worldMatches(event.getWorld())) return;
        chunkKeys = new long[0];
        keysBuiltAt = Long.MIN_VALUE;
        carry.reset();
        depthCache.clear();
        topCache.clear();
    }

    /** The loaded chunks within the radius of any non-spectator player; a chunk near two players is listed once. */
    private void rebuildChunks(WorldServer world) {
        LongOpenHashSet set = new LongOpenHashSet();
        int radius = radius();
        int rc = (radius >> 4) + 1;
        double reach = radius + 11.0D;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator() || player.isDead) continue;
            int pcx = (int) Math.floor(player.posX) >> 4;
            int pcz = (int) Math.floor(player.posZ) >> 4;
            for (int dx = -rc; dx <= rc; dx++) {
                for (int dz = -rc; dz <= rc; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    double ox = cx * 16.0D + 8.0D - player.posX;
                    double oz = cz * 16.0D + 8.0D - player.posZ;
                    if (ox * ox + oz * oz > reach * reach) continue;
                    if (world.getChunkProvider().getLoadedChunk(cx, cz) == null) continue;
                    set.add(((long) cx & 0xFFFFFFFFL) | ((long) cz << 32));
                }
            }
        }
        chunkKeys = set.toLongArray();
    }

    /**
     * Reads a column: its depth in layers (a full 8 layer block continues the stack upward), caching the Y where the
     * next layer would go. -1 when the column cannot hold layers (unloaded, roofed, water, a plant or any non-air
     * block at the surface, no valid base, or filtered out by the subclass).
     */
    private int readColumn(WorldServer world, long col) {
        if (depthCache.containsKey(col)) return depthCache.get(col);
        int x = LayerLeveling.unpackX(col);
        int z = LayerLeveling.unpackZ(col);
        int depth = -1;
        int topY = -1;
        Chunk chunk = world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4);
        if (chunk != null) {
            int y = chunk.getPrecipitationHeight(new BlockPos(x, 0, z)).getY();
            if (columnAllowed(world, chunk, x, y, z)) {
                int d = 0;
                while (y >= 1 && y < 255) {
                    IBlockState st = chunk.getBlockState(x, y, z);
                    if (isLayerBlock(st)) {
                        int layers = layersOf(st);
                        d += layers;
                        if (layers >= LayerStack.PER_BLOCK) {
                            y++;
                            continue;
                        }
                        depth = d;
                        topY = y;
                        break;
                    }
                    // Only air (or what the subclass lists) takes a layer: crops, water, torches and other blocks are
                    // never replaced.
                    pos.setPos(x, y, z);
                    if (canStartLayerIn(st, world, pos) && canPlaceLayer(world, pos)) {
                        depth = d;
                        topY = y;
                    }
                    break;
                }
            }
        }
        depthCache.put(col, depth);
        topCache.put(col, topY);
        return depth;
    }

    private boolean placeOne(WorldServer world, int x, int z) {
        depthCache.clear();
        topCache.clear();
        long start = LayerLeveling.pack(x, z);
        int d0 = readColumn(world, start);
        if (d0 < 0) return false;

        if (windShadow()) {
            int surfaceY = topCache.get(start);
            boolean shadow = LayerShadow.inShadow(surfaceY, shadowAngle(), dist -> {
                int ux = x - (int) Math.round(windX * dist);
                int uz = z - (int) Math.round(windZ * dist);
                Chunk c = world.getChunkProvider().getLoadedChunk(ux >> 4, uz >> 4);
                return c == null ? -1 : c.getPrecipitationHeight(new BlockPos(ux, 0, uz)).getY();
            });
            // In the lee a layer only lands about a third of the time: slower, not never.
            if (shadow && rand.nextFloat() > 0.35F) return false;
        }

        long target = LayerLeveling.walk(start, c -> readColumn(world, c), step(), cap(), LayerLeveling.MAX_HOPS);
        if (target == LayerLeveling.NONE) return false;

        int tx = LayerLeveling.unpackX(target);
        int tz = LayerLeveling.unpackZ(target);
        int ty = topCache.get(target);
        if (ty < 1) return false;
        pos.setPos(tx, ty, tz);
        IBlockState cur = world.getBlockState(pos);
        IBlockState next;
        if (isLayerBlock(cur)) {
            int layers = layersOf(cur);
            if (layers >= LayerStack.PER_BLOCK) return false;
            next = withLayers(cur, layers + 1);
        } else if (canStartLayerIn(cur, world, pos)) {
            next = firstLayer();
        } else {
            return false;
        }
        // Flag 2: tell the clients, no neighbour cascade. Entities are not skipped.
        return world.setBlockState(pos, next, 2);
    }
}
