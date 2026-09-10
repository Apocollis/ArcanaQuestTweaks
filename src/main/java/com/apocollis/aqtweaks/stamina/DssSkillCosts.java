package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DssSkillCosts {

    private static volatile Costs CACHE;

    private DssSkillCosts() {}

    public static void invalidate() {
        CACHE = null;
    }

    public static int costFor(String registryName) {
        ArcanaQuestTweaksConfig.DynamicSwordSkills cfg =
                ArcanaQuestTweaksConfig.StaminaModuleConfig.dynamicSwordSkills;
        if (cfg == null || !cfg.enableSkillCost) return 0;

        Costs costs = CACHE;
        if (costs == null) {
            costs = new Costs(parse(cfg.skillCosts));
            CACHE = costs;
        }
        if (registryName != null) {
            Integer exact = costs.exact.get(registryName);
            if (exact != null) return Math.max(0, exact);
            if (registryName.indexOf(':') < 0) {
                Integer prefixed = costs.exact.get("dynamicswordskills:" + registryName);
                if (prefixed != null) return Math.max(0, prefixed);
            }
            Integer compacted = costs.compact.get(compact(registryName));
            if (compacted != null) return Math.max(0, compacted);
        }
        return Math.max(0, cfg.defaultSkillCost);
    }

    /** Parsed cfg lines plus the compact-key index, rebuilt together whenever the cache is dropped. */
    private static final class Costs {
        final Map<String, Integer> exact;
        final Map<String, Integer> compact;

        Costs(Map<String, Integer> exact) {
            this.exact = exact;
            if (exact.isEmpty()) {
                this.compact = Collections.emptyMap();
            } else {
                Map<String, Integer> index = new HashMap<>(exact.size());
                // putIfAbsent over the same iteration order keeps the first-match-wins behaviour of
                // the linear scan this index replaces.
                for (Map.Entry<String, Integer> entry : exact.entrySet()) {
                    index.putIfAbsent(compact(entry.getKey()), entry.getValue());
                }
                this.compact = index;
            }
        }
    }

    private static String compact(String name) {
        String s = name;
        int colon = s.lastIndexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        return s.replace("_", "").toLowerCase();
    }

    private static Map<String, Integer> parse(String[] entries) {
        if (entries == null || entries.length == 0) return Collections.emptyMap();
        Map<String, Integer> map = new HashMap<>();
        for (String raw : entries) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.lastIndexOf('=');
            if (eq <= 0 || eq == line.length() - 1) continue;
            String name = line.substring(0, eq).trim();
            try {
                map.put(name, Integer.parseInt(line.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }
}
