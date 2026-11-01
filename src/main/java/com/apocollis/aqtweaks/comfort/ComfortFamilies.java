package com.apocollis.aqtweaks.comfort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Block-id family: shape tokens stripped, then the optional {@code families} override. */
public final class ComfortFamilies {

    private static final String[] SUFFIXES = {
            "_double_slab", "_pressure_plate", "_fence_gate", "_trapdoor",
            "_stairs", "_slab", "_wall", "_fence", "_pillar", "_pane", "_button", "_carpet", "_planks", "_door"
    };

    private ComfortFamilies() {}

    public static String family(String blockId) {
        if (blockId == null || blockId.isEmpty()) return "";
        String mapped = ComfortSystemHandler.FAMILIES.get(blockId);
        if (mapped != null && !mapped.isEmpty()) return mapped;
        int colon = blockId.indexOf(':');
        String namespace = colon >= 0 ? blockId.substring(0, colon + 1) : "";
        String name = colon >= 0 ? blockId.substring(colon + 1) : blockId;
        if (name.startsWith("doubleslab_")) {
            name = name.substring("doubleslab_".length());
        }
        for (int pass = 0; pass < 4; pass++) {
            boolean stripped = false;
            for (String suffix : SUFFIXES) {
                if (name.endsWith(suffix) && name.length() > suffix.length()) {
                    name = name.substring(0, name.length() - suffix.length());
                    stripped = true;
                    break;
                }
            }
            if (!stripped) break;
        }
        return namespace + name;
    }

    /** Families that contain more than one block id. */
    public static Map<String, List<String>> merges() {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>(ComfortSystemHandler.COZY_BLOCKS.keySet());
        Collections.sort(ids);
        for (String id : ids) {
            grouped.computeIfAbsent(family(id), k -> new ArrayList<>()).add(id);
        }
        Map<String, List<String>> merges = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                merges.put(entry.getKey(), entry.getValue());
            }
        }
        return merges;
    }
}
