package com.apocollis.aqtweaks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GSON-compatible data class representing the comfort system JSON configuration.
 * Fields are populated automatically by Gson.fromJson().
 */
public class ComfortConfig {
    public Map<String, Integer> category_limits = new LinkedHashMap<>();
    public float pet_comfort_value = 3.0f;
    public float threshold_homestead_1 = 5.0f;
    public float threshold_homestead_2 = 15.0f;
    public float threshold_homestead_3 = 30.0f;
    public Map<String, Map<String, Float>> categories = new LinkedHashMap<>();
}
