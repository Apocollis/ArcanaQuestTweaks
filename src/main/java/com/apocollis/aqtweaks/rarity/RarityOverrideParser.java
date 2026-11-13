package com.apocollis.aqtweaks.rarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the {@code Custom Item Rarities} lines. Pure (no Minecraft types). Format {@code modid:item=TIER},
 * {@code modid:item:meta=TIER} or {@code modid:*=TIER}.
 */
public final class RarityOverrideParser {
    private RarityOverrideParser() {}

    public static final class Table {
        private final Map<String, RarityTier> byKey = new HashMap<>();

        public boolean isEmpty() {
            return byKey.isEmpty();
        }

        public int size() {
            return byKey.size();
        }

        /** Exact modid:item:meta, then modid:item, then modid:*. Returns null when nothing matches. */
        public RarityTier lookup(String modid, String item, int meta) {
            RarityTier t = byKey.get(modid + ":" + item + ":" + meta);
            if (t != null) return t;
            t = byKey.get(modid + ":" + item);
            if (t != null) return t;
            return byKey.get(modid + ":*");
        }
    }

    public static final class Result {
        public final Table table = new Table();
        public final List<String> warnings = new ArrayList<>();
    }

    public static Result parse(String[] lines) {
        Result r = new Result();
        if (lines == null) return r;
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) {
                r.warnings.add("missing '=' in \"" + line + "\"");
                continue;
            }
            String key = line.substring(0, eq).trim();
            RarityTier tier = RarityTier.parse(line.substring(eq + 1));
            if (tier == null) {
                r.warnings.add("unknown tier in \"" + line + "\"");
                continue;
            }
            String[] parts = key.split(":", -1);
            if (parts.length < 2 || parts.length > 3 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                r.warnings.add("bad item key in \"" + line + "\"");
                continue;
            }
            String modid = parts[0].trim();
            String item = parts[1].trim();
            String stored;
            if (parts.length == 3) {
                if (item.equals("*")) {
                    r.warnings.add("wildcard cannot take a meta in \"" + line + "\"");
                    continue;
                }
                int meta;
                try {
                    meta = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    r.warnings.add("bad meta in \"" + line + "\"");
                    continue;
                }
                if (meta < 0) {
                    r.warnings.add("negative meta in \"" + line + "\"");
                    continue;
                }
                stored = modid + ":" + item + ":" + meta;
            } else {
                stored = modid + ":" + item;
            }
            r.table.byKey.put(stored, tier);
        }
        return r;
    }
}
