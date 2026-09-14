package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.monster.IMob;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * After InControl {@code PotentialSpawns}, drop surface-only ids in caves and underground-only ids on the surface.
 */
public class SpawnLayerFilter {

    private static volatile boolean structureExemption;

    public static void enableStructureExemption() {
        structureExemption = true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        var general = SpawningModuleConfig.general;
        if (!general.enable) {
            return;
        }
        World world = event.getWorld();
        if (world == null || world.isRemote) {
            return;
        }
        if (general.overworldOnly && world.provider.getDimension() != 0) {
            return;
        }
        if (general.monsterOnly && event.getType() != EnumCreatureType.MONSTER) {
            return;
        }
        List<Biome.SpawnListEntry> list = event.getList();
        if (list == null || list.isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (general.filterPotentialSpawns && SpawnTypeLists.ready() && pos != null) {
            boolean cave = isCavePick(world, pos);
            Set<String> structuresHere = cave && structureExemption
                    ? SpawnStructureExemption.structuresAt(world, pos)
                    : Collections.emptySet();
            list.removeIf(entry -> shouldStrip(entry, cave, structuresHere));
        }
        keepLastPerClass(list);
    }

    /**
     * {@code MixinWorldEntitySpawner} clears the pack context at {@code RETURN}, which an exception
     * thrown out of {@code findChunksForSpawning} would skip — leaving the spawner flag stuck true on
     * this thread and mis-attributing later spawns. The spawner only ever runs inside a world tick,
     * so clearing here bounds any leak to the tick it happened in.
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && SpawnPackContext.inSpawner()) {
            SpawnPackContext.leaveSpawner();
        }
    }

    /**
     * InControl appends group-count rows and leaves vanilla 4–4 in place. Keep the last row per class.
     */
    private static void keepLastPerClass(List<Biome.SpawnListEntry> list) {
        if (list.size() < 2) {
            return;
        }
        Map<Class<?>, Biome.SpawnListEntry> last = new LinkedHashMap<>();
        for (Biome.SpawnListEntry entry : list) {
            if (entry == null || entry.entityClass == null) {
                continue;
            }
            last.put(entry.entityClass, entry);
        }
        if (last.isEmpty()) {
            return;
        }
        list.clear();
        list.addAll(last.values());
    }

    public static boolean isCavePick(World world, BlockPos pos) {
        var general = SpawningModuleConfig.general;
        return pos.getY() < general.caveMaxY
                && world.getLightFor(EnumSkyBlock.SKY, pos) <= general.maxCaveSkyLight;
    }

    /**
     * Same strip as {@link WorldEvent.PotentialSpawns} at this pos. Used so mixed-group companions
     * are not placed where the layer filter would have removed that id.
     */
    public static boolean wouldStrip(World world, BlockPos pos, EntityLiving living) {
        var general = SpawningModuleConfig.general;
        if (!general.enable || !general.filterPotentialSpawns || !SpawnTypeLists.ready()
                || world == null || pos == null || living == null) {
            return false;
        }
        if (general.overworldOnly && world.provider.getDimension() != 0) {
            return false;
        }
        if (general.monsterOnly && !(living instanceof IMob)) {
            return false;
        }
        ResourceLocation key = EntityList.getKey(living);
        if (key == null) {
            return false;
        }
        boolean cave = isCavePick(world, pos);
        Set<String> structuresHere = cave && structureExemption
                ? SpawnStructureExemption.structuresAt(world, pos)
                : Collections.emptySet();
        return shouldStripId(key.toString(), cave, structuresHere);
    }

    private static boolean shouldStrip(Biome.SpawnListEntry entry, boolean cave, Set<String> structuresHere) {
        if (entry == null || entry.entityClass == null) {
            return false;
        }
        ResourceLocation key = EntityList.getKey(entry.entityClass);
        if (key == null) {
            return false;
        }
        return shouldStripId(key.toString(), cave, structuresHere);
    }

    private static boolean shouldStripId(String id, boolean cave, Set<String> structuresHere) {
        if (cave) {
            if (!SpawnTypeLists.surfaceOnly().contains(id)) {
                return false;
            }
            return !(structureExemption && SpawnStructureExemption.keepSurfaceMob(id, structuresHere));
        }
        return SpawnTypeLists.undergroundOnly().contains(id);
    }
}
