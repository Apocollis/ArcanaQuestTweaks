package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import java.util.List;
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

/**
 * After InControl {@code PotentialSpawns}, drop surface-only ids in caves and underground-only ids on the surface.
 */
public class SpawnLayerFilter {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        var general = SpawningModuleConfig.general;
        if (!general.enable || !general.filterPotentialSpawns || !SpawnTypeLists.ready()) {
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
        if (pos == null) {
            return;
        }
        boolean cave = isCavePick(world, pos);
        list.removeIf(entry -> shouldStrip(entry, cave));
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
        return shouldStripId(key.toString(), isCavePick(world, pos));
    }

    private static boolean shouldStrip(Biome.SpawnListEntry entry, boolean cave) {
        if (entry == null || entry.entityClass == null) {
            return false;
        }
        ResourceLocation key = EntityList.getKey(entry.entityClass);
        if (key == null) {
            return false;
        }
        String id = key.toString();
        return shouldStripId(id, cave);
    }

    private static boolean shouldStripId(String id, boolean cave) {
        if (cave) {
            return SpawnTypeLists.surfaceOnly().contains(id);
        }
        return SpawnTypeLists.undergroundOnly().contains(id);
    }
}
