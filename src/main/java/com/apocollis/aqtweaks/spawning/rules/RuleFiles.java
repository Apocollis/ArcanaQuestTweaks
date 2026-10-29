package com.apocollis.aqtweaks.spawning.rules;

import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.apocollis.aqtweaks.spawning.table.SpawnTableSources;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Checks the three rule files of a pack together, with the biome categories of its {@code biome_mappings.json}. */
public final class RuleFiles {
    /** What the check found: one line per file, then every problem. */
    public static final class Report {
        public final List<String> lines = new ArrayList<String>();
        public int problems;
        public int filesPresent;

        public boolean ok() {
            return problems == 0;
        }
    }

    private RuleFiles() {
    }

    public static Report check(SpawnTableSources.FileReader files) throws IOException {
        Report report = new Report();
        Set<String> categories = new LinkedHashSet<String>();
        String mappings = files.read("biome_mappings.json");
        if (mappings == null) {
            report.lines.add("biome_mappings.json not found: 'biomecat' names cannot be checked");
        } else {
            JsonElement parsed = LenientJson.parse(mappings);
            if (parsed.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                    categories.add(entry.getKey());
                }
            }
        }
        for (RuleKind kind : RuleKind.values()) {
            String text = files.read(kind.fileName);
            if (text == null) {
                report.lines.add(kind.fileName + ": not present");
                continue;
            }
            report.filesPresent++;
            RuleFileLoader.Result result = RuleFileLoader.load(kind, text, categories);
            report.lines.add(kind.fileName + ": " + result.rows.size() + " rules ok"
                    + (result.ok() ? "" : ", " + result.problems.size() + " problems"));
            for (RuleProblem problem : result.problems) {
                report.lines.add("  " + problem);
                report.problems++;
            }
        }
        return report;
    }
}
