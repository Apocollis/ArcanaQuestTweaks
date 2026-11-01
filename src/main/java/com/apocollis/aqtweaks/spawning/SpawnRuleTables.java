package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.spawning.rules.GearTable;
import com.apocollis.aqtweaks.spawning.rules.ModifierTable;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The modifiers and gear tables, read from {@code spawn_modifiers.json} and {@code spawn_gear.json} in the pack folder. */
public final class SpawnRuleTables {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");

    private static volatile ModifierTable modifiers;
    private static volatile GearTable gear;
    private static volatile String status = "not loaded";

    private SpawnRuleTables() {
    }

    /** The modifiers table, or null when the file is missing or has problems. */
    public static ModifierTable modifiers() {
        return modifiers;
    }

    /** The gear table, or null when the file is missing or has problems. */
    public static GearTable gear() {
        return gear;
    }

    /** True when both tables are loaded. */
    public static boolean ready() {
        return modifiers != null && gear != null;
    }

    public static String status() {
        return status;
    }

    /** Reads both files; a file with any problem is not used. Returns a one-line report. */
    public static String reload() {
        modifiers = null;
        gear = null;
        File dir = PotentialSpawnTable.packDirectory();
        if (dir == null) {
            return status = "no config directory";
        }
        try {
            Map<String, List<String>> categories = new LinkedHashMap<String, List<String>>();
            File mappings = new File(dir, "biome_mappings.json");
            if (mappings.isFile()) {
                JsonElement parsed = LenientJson.parse(read(mappings));
                for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                    List<String> biomes = new ArrayList<String>();
                    for (JsonElement biome : entry.getValue().getAsJsonArray()) {
                        biomes.add(biome.getAsString());
                    }
                    categories.put(entry.getKey(), biomes);
                }
            }
            ModifierTable loadedModifiers = null;
            GearTable loadedGear = null;
            List<String> report = new ArrayList<String>();
            RuleFileLoader.Result modifierResult = load(RuleKind.MODIFIERS, dir, categories, report);
            if (modifierResult != null) {
                loadedModifiers = ModifierTable.compile(modifierResult.rows, categories);
                for (String note : loadedModifiers.notes()) {
                    LOGGER.info("[AQTweaks] spawn modifiers: {}", note);
                }
                report.add("modifiers: " + loadedModifiers.size() + " rows");
            }
            RuleFileLoader.Result gearResult = load(RuleKind.GEAR, dir, categories, report);
            if (gearResult != null) {
                loadedGear = GearTable.compile(gearResult.rows, categories);
                report.add("gear: " + loadedGear.size() + " rows");
            }
            if (loadedModifiers != null && loadedGear != null) {
                modifiers = loadedModifiers;
                gear = loadedGear;
            }
            return status = String.join("; ", report);
        } catch (IOException | RuntimeException failure) {
            LOGGER.warn("[AQTweaks] Could not load the modifiers and gear files.", failure);
            return status = "not loaded: " + failure.getMessage();
        }
    }

    private static RuleFileLoader.Result load(RuleKind kind, File dir, Map<String, List<String>> categories,
                                              List<String> report) throws IOException {
        File file = new File(dir, kind.fileName);
        if (!file.isFile()) {
            report.add(file.getName() + " not found");
            return null;
        }
        RuleFileLoader.Result result = RuleFileLoader.load(kind, read(file), categories.keySet());
        if (!result.ok()) {
            for (RuleProblem problem : result.problems) {
                LOGGER.warn("[AQTweaks] {}", problem);
            }
            report.add(result.problems.size() + " problems in " + file.getName() + "; see the log or /aqspawntable check");
            return null;
        }
        return result;
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
