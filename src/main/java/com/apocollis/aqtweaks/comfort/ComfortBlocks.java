package com.apocollis.aqtweaks.comfort;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gson DTO for {@code aqtweaks_comfort_blocks.json}.
 */
public class ComfortBlocks {
    public Map<String, Integer> category_limits = new LinkedHashMap<>();
    public Map<String, Map<String, Float>> categories = new LinkedHashMap<>();
}
