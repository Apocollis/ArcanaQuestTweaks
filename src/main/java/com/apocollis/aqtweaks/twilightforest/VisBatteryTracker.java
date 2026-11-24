package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.mixin.vanilla.AccessorChunkProviderServer;
import com.apocollis.aqtweaks.nether.LoadedBlocks;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.blocks.devices.BlockVisBattery;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Finds Thaumcraft Vis Batteries (blocks with a {@code CHARGE} property, no tile entity) in loaded Twilight
 * Forest chunks without scanning every chunk every pulse. A chunk is scanned once, one section per tick, when it
 * loads; place and break events keep the set current; the set is dropped on unload. At each charging pulse an
 * exposed battery gains levels and pollutes the chunk aura with flux (Thaumcraft's own price for power).
 */
public class VisBatteryTracker {
    private final Long2ObjectMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap<>();
    private final ArrayDeque<long[]> scanQueue = new ArrayDeque<>();
    private final Random rand = new Random();
    /** Fraction of a level each placed battery has gathered (in memory; a restart only loses the fraction). */
    private final it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap carry = new it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap();
    private Block battery;

    private Block battery() {
        if (battery == null) {
            battery = Block.REGISTRY.getObject(new ResourceLocation("thaumcraft", "vis_battery"));
        }
        return battery;
    }

    public int count() {
        int n = 0;
        for (LongOpenHashSet set : byChunk.values()) n += set.size();
        return n;
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || !LeyChannels.isTf(world)) return;
        long key = ChunkPos.asLong(event.getChunk().x, event.getChunk().z);
        for (int section = 0; section < 16; section++) {
            scanQueue.add(new long[] {key, section});
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        World world = event.getWorld();
        if (world.isRemote || !LeyChannels.isTf(world)) return;
        byChunk.remove(ChunkPos.asLong(event.getChunk().x, event.getChunk().z));
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote || !LeyChannels.isTf(event.getWorld())) return;
        if (event.getPlacedBlock().getBlock() == battery()) {
            add(event.getPos());
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote || !LeyChannels.isTf(event.getWorld())) return;
        LongOpenHashSet set = byChunk.get(ChunkPos.asLong(event.getPos().getX() >> 4, event.getPos().getZ() >> 4));
        if (set != null) set.remove(event.getPos().toLong());
    }

    private void add(BlockPos pos) {
        byChunk.computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), k -> new LongOpenHashSet())
                .add(pos.toLong());
    }

    /** Scans one pending chunk section per tick. */
    @SubscribeEvent
    public void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.world instanceof WorldServer)) return;
        if (!LeyChannels.isTf(event.world) || scanQueue.isEmpty()) return;
        long[] task = scanQueue.poll();
        if (!(event.world.getChunkProvider() instanceof AccessorChunkProviderServer accessor)) return;
        Chunk chunk = accessor.aqtweaks$getLoadedChunks().get(task[0]);
        if (chunk == null) return;
        ExtendedBlockStorage storage = chunk.getBlockStorageArray()[(int) task[1]];
        if (storage == null || storage.isEmpty()) return;
        Block target = battery();
        if (target == null) return;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    if (storage.get(x, y, z).getBlock() == target) {
                        add(new BlockPos(chunk.x * 16 + x, storage.getYLocation() + y, chunk.z * 16 + z));
                    }
                }
            }
        }
    }

    /** One charge cycle (every scour interval) for every tracked battery that is exposed. */
    public void pulse(WorldServer world) {
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        if (!c.chargeVisBatteries || c.batteryLevelsPerCycle <= 0.0D) return;
        for (LongOpenHashSet set : byChunk.values()) {
            LongIterator it = set.iterator();
            while (it.hasNext()) {
                BlockPos pos = BlockPos.fromLong(it.nextLong());
                IBlockState state = LoadedBlocks.peek(world, pos.getX(), pos.getY(), pos.getZ());
                if (state == null) continue;
                if (state.getBlock() != battery()) {
                    it.remove();
                    continue;
                }
                int charge = state.getValue(BlockVisBattery.CHARGE);
                if (charge >= 10) continue;
                if (LeyTerrain.structureShelter(world, pos) || LeyTerrain.roofed(world, pos)) continue;
                double exposure = LeyTerrain.heightExposure(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                if (exposure < c.chargeMinExposure) continue;
                double intensity = LeyTerrain.intensity(LeyTerrain.biomeName(world, pos.getX(), pos.getZ()));
                double add = SurgeMath.flat(c.batteryLevelsPerCycle, exposure, intensity);
                long key = pos.toLong();
                LevelAccumulator.Step step = LevelAccumulator.step(carry.get(key), add, 10 - charge);
                if (step.carry > 0.0D) carry.put(key, step.carry); else carry.remove(key);
                int levels = step.levels;
                if (levels <= 0) continue;
                world.setBlockState(pos, state.withProperty(BlockVisBattery.CHARGE, charge + levels), 3);
                if (c.batteryFluxPerLevel > 0.0D) {
                    AuraHelper.polluteAura(world, pos, (float) (c.batteryFluxPerLevel * levels), false);
                }
            }
        }
    }
}
