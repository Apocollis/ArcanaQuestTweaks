package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DssSkillCosts {

    private static volatile Map<String, Integer> CACHE;

    private DssSkillCosts() {}

    public static void invalidate() {
        CACHE = null;
    }

    public static int costFor(String registryName) {
        ArcanaQuestTweaksConfig.DynamicSwordSkills cfg =
                ArcanaQuestTweaksConfig.StaminaModuleConfig.dynamicSwordSkills;
        if (cfg == null || !cfg.enableSkillCost) return 0;

        Map<String, Integer> map = CACHE;
        if (map == null) {
            map = parse(cfg.skillCosts);
            CACHE = map;
        }
        if (registryName != null) {
            Integer exact = map.get(registryName);
            if (exact != null) return Math.max(0, exact);
            if (registryName.indexOf(':') < 0) {
                Integer prefixed = map.get("dynamicswordskills:" + registryName);
                if (prefixed != null) return Math.max(0, prefixed);
            }
            String compact = compact(registryName);
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (compact.equals(compact(entry.getKey()))) {
                    return Math.max(0, entry.getValue());
                }
            }
        }
        return Math.max(0, cfg.defaultSkillCost);
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
