package com.apocollis.aqtweaks.world;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import rtg.world.biome.BiomeProviderRTG;

/**
 * Gives dimensions RTG does not generate (default: Atum, dimension 17) the same light fix RTG gives its own chunks:
 * RTG's column test (the overhang rule: sky light lower than the brightest neighbour minus 1) and its repair, queued
 * and paced through {@link RtgLightFixQueue}. RTG's own triggers ({@code fixLightingAround}, {@code fixLightingOnLoad})
 * only fire for {@code BiomeProviderRTG}, so those dimensions never got the fix.
 *
 * <p>Triggers: a chunk is requested when it loads, and when a chunk finishes populating (a hook at the end of
 * {@code Chunk.populate}) the chunk and its four diagonal neighbours are requested, as RTG's {@code fixLightingAround}
 * does. The queue merges repeats and waits until a chunk's four diagonal neighbours exist.
 */
public final class NonRtgLightFixTrigger {

    private NonRtgLightFixTrigger() {}

    /** Server world in a configured dimension that RTG does not generate. */
    public static boolean applies(World world) {
        if (world == null || world.isRemote) return false;
        ArcanaQuestTweaksConfig.Surface c = ArcanaQuestTweaksConfig.RtgModuleConfig.surface;
        if (!c.nonRtgLightFixEnable || c.nonRtgLightFixDimensions == null) return false;
        int dim = world.provider.getDimension();
        boolean listed = false;
        for (int id : c.nonRtgLightFixDimensions) {
            if (id == dim) {
                listed = true;
                break;
            }
        }
        return listed && !(world.getBiomeProvider() instanceof BiomeProviderRTG);
    }

    /** Called when a chunk has finished populating (end of {@code Chunk.populate}). */
    public static void onPopulated(World world, Chunk chunk) {
        if (chunk == null || !applies(world)) return;
        RtgLightFixQueue.request(world, chunk.getPos());
        IChunkProvider provider = world.getChunkProvider();
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                Chunk diagonal = provider.getLoadedChunk(chunk.x + dx, chunk.z + dz);
                if (diagonal != null && diagonal.isTerrainPopulated()) {
                    RtgLightFixQueue.request(world, new ChunkPos(diagonal.x, diagonal.z));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static final class Events {
        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            World world = event.getWorld();
            Chunk chunk = event.getChunk();
            if (chunk == null || !applies(world)) return;
            RtgLightFixQueue.request(world, chunk.getPos());
        }
    }
}
