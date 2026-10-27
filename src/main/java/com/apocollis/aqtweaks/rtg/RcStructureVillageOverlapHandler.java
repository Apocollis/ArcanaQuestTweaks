package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.reccomplex.events.StructureGenerationEventLite;
import ivorius.reccomplex.world.gen.feature.StructureGenerator;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.NaturalGeneration;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Recurrent Complex natural structures and villages.
 *
 * <p>A first-time suggest whose box overlaps a village is cancelled before paste, so RC pastes nothing and records
 * nothing. The structure id and box are held. On the next server tick (RC's {@code decorate} has released its chunk
 * locks) one walk of {@code Village RC Relocate Walk} blocks looks for a shifted box outside the village. If one is
 * found the structure is generated there once, with a seed that depends only on the world seed, the structure id and
 * the original box. If none is found, or RC refuses the spot (for example a completed structure is already there),
 * the piece is dropped and Tweaks does not retry. RC village pieces are not this event.
 */
public class RcStructureVillageOverlapHandler {

    private static final Logger LOGGER = LogManager.getLogger(ArcanaQuestTweaks.MODID);
    private static final int STEP = 8;
    private static final int QUEUE_CAP = 16;

    private static final class Held {
        final int dim;
        final String id;
        final StructureBoundingBox box;

        Held(int dim, String id, StructureBoundingBox box) {
            this.dim = dim;
            this.id = id;
            this.box = box;
        }
    }

    private static final ArrayDeque<Held> HELD = new ArrayDeque<Held>();
    /** Set while this handler's own generate runs, so its suggest is judged but never queues another walk. */
    private static boolean relocating;

    @SubscribeEvent
    public void onSuggest(StructureGenerationEventLite.Suggest event) {
        if (event == null || !event.isFirstTime() || event.getWorld() == null) return;
        StructureBoundingBox box = event.getBoundingBox();
        if (box == null) return;
        if (!StructureVillageOverlap.overlapsVillage(event.getWorld(), box)) return;
        event.setCanceled(true);
        LOGGER.info("[AQ-RC] Canceled overlapping natural structure {} at [{},{}]x[{},{}] y={}-{}",
                event.getStructureName(), box.minX, box.maxX, box.minZ, box.maxZ, box.minY, box.maxY);
        VillageDebug.log("[AQ-RC] Canceled overlapping natural structure %s at [%d,%d]x[%d,%d]",
                event.getStructureName(), box.minX, box.maxX, box.minZ, box.maxZ);
        if (relocating) return;
        int walk = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rcRelocateWalk;
        World world = event.getWorld();
        if (walk <= 0 || world.isRemote || !(world instanceof WorldServer)) return;
        synchronized (HELD) {
            if (HELD.size() >= QUEUE_CAP) return;
            HELD.add(new Held(world.provider.getDimension(), event.getStructureName(), copy(box)));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Held held;
        synchronized (HELD) {
            held = HELD.poll();
        }
        if (held == null) return;
        try {
            relocate(held);
        } catch (Throwable t) {
            LOGGER.warn("[AQ-RC] Relocation of {} failed: {}", held.id, t.toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void relocate(Held held) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        WorldServer world = server.getWorld(held.dim);
        if (world == null) return;
        Object registered = StructureRegistry.INSTANCE.get(held.id);
        if (!(registered instanceof Structure)) return;
        Structure structure = (Structure) registered;
        List<NaturalGeneration> natural = structure.generationTypes(NaturalGeneration.class);
        if (natural == null || natural.isEmpty()) return;
        NaturalGeneration info = natural.get(0);

        StructureBoundingBox box = held.box;
        int walk = ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rcRelocateWalk;
        int centerX = (box.minX + box.maxX) / 2;
        int centerZ = (box.minZ + box.maxZ) / 2;
        int[] delta = null;
        for (int[] d : candidates(walk)) {
            StructureBoundingBox moved = new StructureBoundingBox(box.minX + d[0], 0, box.minZ + d[1],
                    box.maxX + d[0], 255, box.maxZ + d[1]);
            if (!allChunksLoaded(world, moved)) continue;
            if (StructureVillageOverlap.overlapsVillage(world, moved)) continue;
            delta = d;
            break;
        }
        if (delta == null) {
            LOGGER.info("[AQ-RC] Dropped {} at [{},{}]x[{},{}]: no spot outside the village within {} blocks",
                    held.id, box.minX, box.maxX, box.minZ, box.maxZ, walk);
            return;
        }

        long seed = world.getSeed() * 31L + held.id.hashCode();
        seed = seed * 31L + box.minX;
        seed = seed * 31L + box.minZ;
        seed = seed * 31L + box.maxX;
        seed = seed * 31L + box.maxZ;

        StructureGenerator generator = new StructureGenerator(structure)
                .world(world)
                .generationInfo(info)
                .seed(Long.valueOf(seed))
                .maturity(StructureSpawnContext.GenerateMaturity.SUGGEST)
                .randomPosition(new BlockSurfacePos(centerX + delta[0], centerZ + delta[1]), info.placer)
                .fromCenter(true);
        boolean ok;
        relocating = true;
        try {
            ok = generator.generate().succeeded();
        } finally {
            relocating = false;
        }
        LOGGER.info("[AQ-RC] {} {} moved ({},{}) to center [{},{}]", held.id, ok ? "placed" : "refused after",
                delta[0], delta[1], centerX + delta[0], centerZ + delta[1]);
        VillageDebug.log("[AQ-RC] relocate %s delta=%d,%d result=%s", held.id, delta[0], delta[1], ok);
    }

    /** Shifts of the original box out to {@code walk}, nearest first; ties keep a fixed direction order. */
    private static List<int[]> candidates(int walk) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        List<int[]> out = new ArrayList<int[]>();
        for (int dist = STEP; dist <= walk; dist += STEP) {
            for (int[] dir : dirs) {
                out.add(new int[]{dir[0] * dist, dir[1] * dist});
            }
        }
        out.sort(Comparator.comparingInt((int[] d) -> Math.max(Math.abs(d[0]), Math.abs(d[1]))));
        return out;
    }

    private static boolean allChunksLoaded(WorldServer world, StructureBoundingBox box) {
        for (int cx = box.minX >> 4; cx <= box.maxX >> 4; cx++) {
            for (int cz = box.minZ >> 4; cz <= box.maxZ >> 4; cz++) {
                if (world.getChunkProvider().getLoadedChunk(cx, cz) == null) return false;
            }
        }
        return true;
    }

    private static StructureBoundingBox copy(StructureBoundingBox b) {
        return new StructureBoundingBox(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }
}
