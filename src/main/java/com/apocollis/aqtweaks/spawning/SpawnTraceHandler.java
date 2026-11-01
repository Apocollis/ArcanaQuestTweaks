package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.spawning.rules.ConditionTable;
import com.apocollis.aqtweaks.spawning.rules.Decision;
import mcjty.incontrol.InControl;
import mcjty.incontrol.rules.RuleCache;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Writes the spawn check and join of watched mobs to the spawn trace, after In Control has acted. */
public final class SpawnTraceHandler {

    static String idOf(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity.getClass());
        return key == null ? null : key.toString();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void spawnCheck(LivingSpawnEvent.CheckSpawn event) {
        String id = idOf(event.getEntity());
        if (!SpawnTrace.watching(id) || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        try {
            BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
            SpawnTrace.line("check  " + id + " " + where(event.getWorld(), pos) + " spawner=" + event.isSpawner()
                    + " context=" + context() + " -> result " + event.getResult() + " | " + counts(event.getWorld(), event.getEntity())
                    + " | table " + tableSays(ConditionTable.Phase.SPAWN, new LiveQuery.Check(event)));
        } catch (RuntimeException failure) {
            SpawnTrace.line("check  " + id + " trace failed: " + failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void join(EntityJoinWorldEvent event) {
        String id = idOf(event.getEntity());
        if (!(event.getEntity() instanceof EntityLiving) || !SpawnTrace.watching(id) || event.getWorld().isRemote) {
            return;
        }
        try {
            BlockPos pos = event.getEntity().getPosition();
            SpawnTrace.line("join   " + id + " " + where(event.getWorld(), pos) + " context=" + context()
                    + " -> " + (event.isCanceled() ? "CANCELLED" : "joined") + " | " + counts(event.getWorld(), event.getEntity())
                    + " | table " + tableSays(ConditionTable.Phase.JOIN, new LiveQuery.Join(event)));
        } catch (RuntimeException failure) {
            SpawnTrace.line("join   " + id + " trace failed: " + failure);
        }
    }

    private static String where(World world, BlockPos pos) {
        net.minecraft.world.biome.Biome biome = world.getBiome(pos);
        ResourceLocation name = biome.getRegistryName();
        return "dim " + world.provider.getDimension() + " (" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
                + ") biome " + name + " sky " + world.canBlockSeeSky(pos) + " time " + (world.getWorldTime() % 24000);
    }

    /** How the mob came about: the vanilla natural-spawn loop, the pack filler or a party, or something else. */
    private static String context() {
        if (SpawnPackContext.filling()) {
            return "pack-fill-or-party";
        }
        return SpawnPackContext.inSpawner() ? "natural-spawn-loop" : "other(egg/command/mod/raid/worldgen)";
    }

    static String counts(World world, Entity entity) {
        RuleCache cache = InControl.setup.cache;
        return "In Control counts: this mob " + cache.getCount(world, entity.getClass()) + ", hostile "
                + cache.getCountHostile(world) + ", players " + cache.getValidPlayers(world);
    }

    private static String tableSays(ConditionTable.Phase phase, LiveQuery query) {
        ConditionTable table = ConditionShadow.conditions();
        if (table == null) {
            return "not loaded";
        }
        Decision decision = table.decide(phase, query);
        return decision + (decision.usedRandom ? " (random row in play)" : "");
    }
}
