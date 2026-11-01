package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.apocollis.aqtweaks.spawning.rules.ConditionTable;
import com.apocollis.aqtweaks.spawning.rules.Decision;
import mcjty.incontrol.InControl;
import mcjty.incontrol.rules.RuleCache;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Writes the spawn check and join of watched mobs to the spawn trace, after In Control has acted. */
public final class SpawnTraceHandler {
    private static String lastDetail = "";

    /** While the trace is on, writes the shadow comparison and the live counts every Spawn Trace Summary Seconds. */
    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SpawnTrace.enabled()) {
            return;
        }
        int seconds = SpawningModuleConfig.general.spawnTraceSummarySeconds;
        if (seconds <= 0 || !SpawnTrace.every("summary", seconds * 1000L)) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            writeSummary(server);
        }
    }

    /**
     * The state of the whole spawning module at this moment: the table, the comparison with In Control (the counts every
     * time, the list of differences when it has changed) and the live counts of the watched mobs.
     */
    static synchronized void writeSummary(MinecraftServer server) {
        try {
            SpawnTrace.line("-- summary: " + PotentialSpawnTable.describe() + "; Spawn Rules Decide "
                    + (SpawnRuleDecider.deciding() ? "ON" : "off"));
            StringBuilder detail = new StringBuilder();
            for (String line : ConditionShadow.summary()) {
                if (line.startsWith("  ")) {
                    detail.append(line).append('\n');
                } else {
                    SpawnTrace.line("shadow " + line);
                }
            }
            if (!detail.toString().equals(lastDetail)) {
                lastDetail = detail.toString();
                for (String line : detail.toString().split("\n")) {
                    if (!line.isEmpty()) {
                        SpawnTrace.line("shadow " + line);
                    }
                }
            } else if (detail.length() > 0) {
                SpawnTrace.line("shadow   (the list of differences is unchanged since the last summary)");
            }
            SpawnTrace.writeTally();
            countsNow(server);
        } catch (RuntimeException failure) {
            SpawnTrace.line("-- summary failed: " + failure);
        }
    }

    /** For each watched mob and world: the mobs really alive in loaded chunks against In Control's count. */
    static int countsNow(MinecraftServer server) {
        int written = 0;
        for (WorldServer world : server.worlds) {
            for (String id : SpawnTrace.describeWatched().split(", ")) {
                net.minecraftforge.fml.common.registry.EntityEntry entry =
                        ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id));
                if (entry == null) {
                    continue;
                }
                Class<? extends Entity> type = entry.getEntityClass();
                int alive = 0;
                for (Entity each : world.loadedEntityList) {
                    if (each.getClass() == type && !each.isDead) {
                        alive++;
                    }
                }
                RuleCache cache = InControl.setup.cache;
                SpawnTrace.line("now    " + id + " dim " + world.provider.getDimension() + ": " + alive
                        + " alive in loaded chunks, In Control's count " + cache.getCount(world, type) + ", players "
                        + cache.getValidPlayers(world));
                written++;
            }
        }
        return written;
    }

    static String idOf(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity.getClass());
        return key == null ? null : key.toString();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void spawnCheck(LivingSpawnEvent.CheckSpawn event) {
        String id = idOf(event.getEntity());
        boolean watched = SpawnTrace.watching(id);
        if (id == null || !(watched || SpawnTrace.others()) || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        try {
            Event.Result atInControl = ConditionShadow.inControlResult(event);
            String result = (atInControl != null ? atInControl : event.getResult()).name();
            String final_ = atInControl != null && atInControl != event.getResult()
                    ? " (final " + event.getResult().name() + ", changed after In Control)" : "";
            String context = context();
            Decision decision = tableDecision(ConditionTable.Phase.SPAWN, new LiveQuery.Check(event));
            String table = describe(decision);
            boolean differs = decision != null && !decision.usedRandom
                    && !(decision.result == null ? (atInControlBefore(event, atInControl)) : ConditionShadow.toEvent(decision.result)).name().equals(result);
            SpawnTrace.count(id, "check:" + result + (differs ? "(table differs)" : ""));
            SpawnTrace.count(id, "via:" + context);
            if (differs) {
                SpawnTrace.escalate(id, "at the spawn check In Control left " + result + " but the conditions table says " + table);
            }
            if (watched || differs) {
                BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
                SpawnTrace.line("check  " + id + " " + where(event.getWorld(), pos) + " spawner=" + event.isSpawner()
                        + " context=" + context + " -> In Control left " + result + final_ + (differs ? "  ** TABLE DIFFERS **" : "")
                        + " | " + counts(event.getWorld(), event.getEntity()) + " | table " + table);
            }
        } catch (RuntimeException failure) {
            SpawnTrace.line("check  " + id + " trace failed: " + failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void join(EntityJoinWorldEvent event) {
        String id = idOf(event.getEntity());
        boolean watched = SpawnTrace.watching(id);
        if (!(event.getEntity() instanceof EntityLiving) || id == null || !(watched || SpawnTrace.others())
                || event.getWorld().isRemote) {
            return;
        }
        try {
            String context = context();
            Decision decision = tableDecision(ConditionTable.Phase.JOIN, new LiveQuery.Join(event));
            boolean tableCancels = decision != null && decision.result == Decision.Result.DENY;
            Boolean inControlCancelled = ConditionShadow.inControlCancelled(event);
            boolean cancelledByInControl = inControlCancelled != null ? inControlCancelled.booleanValue() : event.isCanceled();
            boolean differs = decision != null && !decision.usedRandom && tableCancels != cancelledByInControl;
            SpawnTrace.count(id, event.isCanceled() ? "join:cancelled" : "join:ok");
            if (differs) {
                SpawnTrace.count(id, "join:table differs");
            }
            if (differs) {
                SpawnTrace.escalate(id, "at join In Control " + (cancelledByInControl ? "cancelled" : "let it in")
                        + " but the conditions table says " + describe(decision));
            }
            if (watched || differs) {
                BlockPos pos = event.getEntity().getPosition();
                SpawnTrace.line("join   " + id + " " + where(event.getWorld(), pos) + " context=" + context
                        + " -> " + (event.isCanceled() ? "CANCELLED" : "joined") + (differs ? "  ** TABLE DIFFERS **" : "")
                        + " | " + counts(event.getWorld(), event.getEntity()) + " | table " + describe(decision));
            }
        } catch (RuntimeException failure) {
            SpawnTrace.line("join   " + id + " trace failed: " + failure);
        }
    }

    /** What the result was before In Control acted, which only the shadow knows; the event's own result when it is not known. */
    private static Event.Result atInControlBefore(LivingSpawnEvent.CheckSpawn event, Event.Result atInControl) {
        return atInControl != null ? ConditionShadow.resultBeforeInControl() : event.getResult();
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

    private static Decision tableDecision(ConditionTable.Phase phase, LiveQuery query) {
        ConditionTable table = ConditionShadow.conditions();
        return table == null ? null : table.decide(phase, query);
    }

    private static String describe(Decision decision) {
        if (decision == null) {
            return "not loaded";
        }
        return decision + (decision.usedRandom ? " (random row in play)" : "");
    }
}
