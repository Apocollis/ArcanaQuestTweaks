package com.apocollis.aqtweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles loading, default generation, and application of the comfort JSON configuration.
 * Called during FMLPreInitializationEvent to populate ComfortSystemHandler's lookup maps.
 */
public class ComfortConfigLoader {

    public static void load(File configDir) {
        File configFile = new File(configDir, "arcanaquesttweaks_comfort.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            if (!configFile.exists()) {
                // Generate default config file on first launch
                ComfortConfig config = createDefaults();
                try (FileWriter writer = new FileWriter(configFile)) {
                    gson.toJson(config, writer);
                }
                apply(config);
            } else {
                // Load existing config from disk
                try (FileReader reader = new FileReader(configFile)) {
                    ComfortConfig config = gson.fromJson(reader, ComfortConfig.class);
                    apply(config);
                }
            }
        } catch (Exception e) {
            System.err.println("[ArcanaQuestTweaks] Failed to load comfort config, using defaults.");
            e.printStackTrace();
            apply(createDefaults());
        }
    }

    private static ComfortConfig createDefaults() {
        ComfortConfig config = new ComfortConfig();

        // Category limits (max items that count per category)
        config.category_limits.put("hearth", 1);
        config.category_limits.put("bedding", 1);
        config.category_limits.put("seating", 2);
        config.category_limits.put("lighting", 3);
        config.category_limits.put("study", 2);
        config.category_limits.put("decoration", 4);
        config.category_limits.put("nature", 3);
        config.category_limits.put("structure", 8);
        config.category_limits.put("pets", 2);

        // Pet comfort value per tamed companion
        config.pet_comfort_value = 3.0f;

        // Homestead comfort score thresholds
        config.threshold_homestead_1 = 5.0f;
        config.threshold_homestead_2 = 15.0f;
        config.threshold_homestead_3 = 30.0f;

        // --- Hearth (Warmth & Cooking) ---
        Map<String, Float> hearth = new LinkedHashMap<>();
        hearth.put("farmersdelight:stove", 4.0f);
        config.categories.put("hearth", hearth);

        // --- Bedding (Resting & Sleep) ---
        Map<String, Float> bedding = new LinkedHashMap<>();
        bedding.put("comforts:hammock", 3.5f);
        bedding.put("minecraft:bed", 3.0f);
        bedding.put("comforts:sleeping_bag", 2.0f);
        config.categories.put("bedding", bedding);

        // --- Seating (Comfort & Relaxation) ---
        Map<String, Float> seating = new LinkedHashMap<>();
        seating.put("bibliocraft:seat", 3.0f);
        config.categories.put("seating", seating);

        // --- Lighting (Atmosphere) ---
        Map<String, Float> lighting = new LinkedHashMap<>();
        lighting.put("saltmod:salt_lamp", 2.0f);
        lighting.put("fancylamps:gothic_lamp", 2.0f);
        lighting.put("rustic:iron_lantern", 2.0f);
        config.categories.put("lighting", lighting);

        // --- Study (Mental Focus & Magic) ---
        Map<String, Float> study = new LinkedHashMap<>();
        study.put("bibliocraft:bookcase", 1.5f);
        study.put("inspirations:bookshelf", 1.5f);
        config.categories.put("study", study);

        // --- Decoration (Aesthetics) ---
        Map<String, Float> decoration = new LinkedHashMap<>();
        decoration.put("minecraft:carpet", 1.0f);
        decoration.put("minecraft:standing_banner", 1.5f);
        decoration.put("minecraft:wall_banner", 1.5f);
        config.categories.put("decoration", decoration);

        // --- Nature (Plants & Greenery) ---
        Map<String, Float> nature = new LinkedHashMap<>();
        nature.put("minecraft:flower_pot", 1.5f);
        nature.put("minecraft:red_flower", 1.0f);
        nature.put("minecraft:yellow_flower", 1.0f);
        config.categories.put("nature", nature);

        // --- Structure (Walls & Flooring) ---
        Map<String, Float> structure = new LinkedHashMap<>();
        structure.put("rustic:slate_chiseled", 1.0f);
        structure.put("earthworks:block_plaster", 1.0f);
        structure.put("earthworks:block_adobe", 1.0f);
        structure.put("earthworks:block_cob", 1.0f);
        config.categories.put("structure", structure);

        return config;
    }

    private static void apply(ComfortConfig config) {
        ComfortSystemHandler.CATEGORY_LIMITS.clear();
        ComfortSystemHandler.CATEGORY_LIMITS.putAll(config.category_limits);

        ComfortSystemHandler.PET_COMFORT_VALUE = config.pet_comfort_value;

        ComfortSystemHandler.THRESHOLD_HOMESTEAD_1 = config.threshold_homestead_1;
        ComfortSystemHandler.THRESHOLD_HOMESTEAD_2 = config.threshold_homestead_2;
        ComfortSystemHandler.THRESHOLD_HOMESTEAD_3 = config.threshold_homestead_3;

        ComfortSystemHandler.COZY_BLOCKS.clear();
        for (Map.Entry<String, Map<String, Float>> categoryEntry : config.categories.entrySet()) {
            String category = categoryEntry.getKey();
            Map<String, Float> blocksMap = categoryEntry.getValue();

            if (blocksMap != null) {
                for (Map.Entry<String, Float> blockEntry : blocksMap.entrySet()) {
                    String blockId = blockEntry.getKey();
                    float weight = blockEntry.getValue();
                    ComfortSystemHandler.COZY_BLOCKS.put(blockId,
                        new ComfortSystemHandler.CozyConfig(weight, category));
                }
            }
        }
    }
}
