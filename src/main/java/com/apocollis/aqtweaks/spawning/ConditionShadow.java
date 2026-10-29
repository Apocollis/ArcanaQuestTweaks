package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.apocollis.aqtweaks.spawning.rules.ConditionTable;
import com.apocollis.aqtweaks.spawning.rules.Decision;
import com.apocollis.aqtweaks.spawning.rules.RuleFileLoader;
import com.apocollis.aqtweaks.spawning.rules.RuleKind;
import com.apocollis.aqtweaks.spawning.rules.RuleProblem;
import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.EntityLiving;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs the conditions table beside In Control without deciding anything. After In Control has handled a spawn check or a
 * join, the table decides the same spawn and the two outcomes are compared; every difference is counted and kept for
 * {@code /aqspawntable shadow}. Spawns where a 'random' row was tried are not compared, since each side rolls its own.
 * Registered after In Control, so its LOWEST handlers run after In Control's.
 */
public final class ConditionShadow {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final int MAX_KEPT = 300;

    private static final class Mismatch {
        final String example;
        int count;

        Mismatch(String example) {
            this.example = example;
        }
    }

    private static volatile ConditionTable table;
    private static volatile boolean ready;
    private static final Map<String, Mismatch> MISMATCHES = new LinkedHashMap<String, Mismatch>();
    private static long same;
    private static long different;
    private static long skippedRandom;
    private static long skippedOther;
    private static long unlogged;
    private static boolean failureLogged;

    private Event.Result before = Event.Result.DEFAULT;
    private boolean cancelledBefore;

    /** Reads spawn_conditions.json from the pack folder; the shadow stays off when it is missing or has problems. */
    public static String reload() {
        File dir = PotentialSpawnTable.packDirectory();
        if (dir == null) {
            ready = false;
            return "no config directory";
        }
        File file = new File(dir, RuleKind.CONDITIONS.fileName);
        if (!file.isFile()) {
            ready = false;
            table = null;
            return file.getName() + " not found";
        }
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Map<String, List<String>> categories = new LinkedHashMap<String, List<String>>();
            File mappings = new File(dir, "biome_mappings.json");
            if (mappings.isFile()) {
                JsonElement parsed = LenientJson.parse(new String(Files.readAllBytes(mappings.toPath()),
                        StandardCharsets.UTF_8));
                for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                    List<String> biomes = new ArrayList<String>();
                    for (JsonElement biome : entry.getValue().getAsJsonArray()) {
                        biomes.add(biome.getAsString());
                    }
                    categories.put(entry.getKey(), biomes);
                }
            }
            RuleFileLoader.Result result = RuleFileLoader.load(RuleKind.CONDITIONS, text, categories.keySet());
            if (!result.ok()) {
                for (RuleProblem problem : result.problems) {
                    LOGGER.warn("[AQTweaks] {}", problem);
                }
                ready = false;
                table = null;
                return result.problems.size() + " problems in " + file.getName() + "; see the log or /aqspawntable check";
            }
            table = ConditionTable.compile(result.rows, categories);
            ready = true;
            for (String note : table.notes()) {
                LOGGER.info("[AQTweaks] spawn conditions: {}", note);
            }
            return "loaded " + table.size() + " rules";
        } catch (IOException | RuntimeException failure) {
            LOGGER.warn("[AQTweaks] Could not load {}.", file.getAbsolutePath(), failure);
            ready = false;
            table = null;
            return "not loaded: " + failure.getMessage();
        }
    }

    private static boolean active() {
        return ready && SpawningModuleConfig.general.enable && SpawningModuleConfig.general.shadowSpawnConditions;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void spawnCheckBefore(LivingSpawnEvent.CheckSpawn event) {
        if (active()) {
            before = event.getResult();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void spawnCheckAfter(LivingSpawnEvent.CheckSpawn event) {
        if (!active() || event.getWorld() == null || event.getWorld().isRemote) {
            return;
        }
        try {
            ConditionTable current = table;
            Decision decision = current.decide(ConditionTable.Phase.SPAWN, new LiveQuery.Check(event));
            if (decision.usedRandom) {
                skippedRandom++;
                return;
            }
            Event.Result expected = decision.result == null ? before : toEvent(decision.result);
            record("spawn", event.getEntity(), event.getWorld().provider.getDimension(), (int) event.getY(),
                    event.getResult().name(), expected.name(), decision, event.getWorld().getWorldTime());
        } catch (RuntimeException failure) {
            failed(failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void joinBefore(EntityJoinWorldEvent event) {
        if (active()) {
            cancelledBefore = event.isCanceled();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void joinAfter(EntityJoinWorldEvent event) {
        if (!active() || !(event.getEntity() instanceof EntityLiving) || event.getWorld().isRemote) {
            return;
        }
        if (cancelledBefore) {
            skippedOther++;
            return;
        }
        try {
            Decision decision = table.decide(ConditionTable.Phase.JOIN, new LiveQuery.Join(event));
            if (decision.usedRandom) {
                skippedRandom++;
                return;
            }
            boolean cancel = decision.result == Decision.Result.DENY;
            record("join", event.getEntity(), event.getWorld().provider.getDimension(),
                    event.getEntity().getPosition().getY(), event.isCanceled() ? "CANCEL" : "ok",
                    cancel ? "CANCEL" : "ok", decision, event.getWorld().getWorldTime());
        } catch (RuntimeException failure) {
            failed(failure);
        }
    }

    private static Event.Result toEvent(Decision.Result result) {
        switch (result) {
            case ALLOW:
                return Event.Result.ALLOW;
            case DENY:
                return Event.Result.DENY;
            default:
                return Event.Result.DEFAULT;
        }
    }

    private static synchronized void record(String phase, net.minecraft.entity.Entity entity, int dimension, int y, String inControl,
                               String tweaks, Decision decision, long time) {
        if (inControl.equals(tweaks)) {
            same++;
            return;
        }
        different++;
        String id = net.minecraft.entity.EntityList.getKey(entity.getClass()) + "";
        String key = phase + " " + id + " In Control " + inControl + ", Tweaks " + tweaks
                + (decision.row >= 0 ? " (conditions row " + decision.row + ")" : " (no row)");
        Mismatch known = MISMATCHES.get(key);
        if (known != null) {
            known.count++;
        } else if (MISMATCHES.size() < MAX_KEPT) {
            Mismatch added = new Mismatch("dimension " + dimension + ", y " + y + ", time " + (int) (time % 24000));
            added.count = 1;
            MISMATCHES.put(key, added);
            LOGGER.info("[AQTweaks] spawn conditions differ from In Control: {} at {}", key, added.example);
        } else {
            unlogged++;
        }
    }

    private static void failed(RuntimeException failure) {
        skippedOther++;
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("[AQTweaks] Spawn condition comparison failed; further failures are counted only.", failure);
        }
    }

    public static synchronized void reset() {
        MISMATCHES.clear();
        same = 0;
        different = 0;
        skippedRandom = 0;
        skippedOther = 0;
        unlogged = 0;
    }

    /** The comparison so far, one line each. */
    public static synchronized List<String> summary() {
        List<String> lines = new ArrayList<String>();
        lines.add("Spawn conditions " + (ready ? "loaded (" + table.size() + " rules)" : "not loaded")
                + ", comparison " + (SpawningModuleConfig.general.shadowSpawnConditions ? "on" : "off") + ": " + same
                + " same, " + different + " different, " + skippedRandom + " skipped (random rows), " + skippedOther
                + " skipped (other).");
        for (Map.Entry<String, Mismatch> entry : MISMATCHES.entrySet()) {
            lines.add("  " + entry.getValue().count + "x " + entry.getKey() + "; first at " + entry.getValue().example);
        }
        if (unlogged > 0) {
            lines.add("  " + unlogged + " more differences of kinds not listed (" + MAX_KEPT + " kinds kept)");
        }
        return lines;
    }
}
