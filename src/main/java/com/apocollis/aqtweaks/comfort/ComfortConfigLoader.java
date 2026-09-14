package com.apocollis.aqtweaks.comfort;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@code aqtweaks_comfort_settings.json} and {@code aqtweaks_comfort_blocks.json}.
 * Does not read the legacy combined {@code aqtweaks_comfort.json}.
 */
public class ComfortConfigLoader {

    private static final String SETTINGS_NAME = "aqtweaks_comfort_settings.json";
    private static final String BLOCKS_NAME = "aqtweaks_comfort_blocks.json";

    public static void load(File configDir) {
        File subDir = new File(configDir, "arcanaquesttweaks");
        if (!subDir.exists()) {
            subDir.mkdirs();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        loadSettings(new File(subDir, SETTINGS_NAME), gson);
        loadBlocks(new File(subDir, BLOCKS_NAME), gson);
    }

    private static void loadSettings(File settingsFile, Gson gson) {
        try {
            ComfortSettings settings;
            if (!settingsFile.exists()) {
                settings = new ComfortSettings();
                try (FileWriter writer = new FileWriter(settingsFile)) {
                    gson.toJson(settings, writer);
                }
            } else {
                try (FileReader reader = new FileReader(settingsFile)) {
                    settings = gson.fromJson(reader, ComfortSettings.class);
                }
                if (settings == null) {
                    settings = new ComfortSettings();
                }
            }
            applySettings(settings);
        } catch (Exception e) {
            System.err.println("[ArcanaQuestTweaks] Failed to load comfort settings, using defaults.");
            e.printStackTrace();
            applySettings(new ComfortSettings());
        }
    }

    private static void loadBlocks(File blocksFile, Gson gson) {
        try {
            ComfortBlocks blocks;
            if (!blocksFile.exists()) {
                blocks = createBlocksDefaults();
                try (FileWriter writer = new FileWriter(blocksFile)) {
                    gson.toJson(blocks, writer);
                }
            } else {
                try (FileReader reader = new FileReader(blocksFile)) {
                    blocks = gson.fromJson(reader, ComfortBlocks.class);
                }
                if (blocks == null) {
                    blocks = createBlocksDefaults();
                }
                if (mergeMissingCrafting(blocks)) {
                    try (FileWriter writer = new FileWriter(blocksFile)) {
                        gson.toJson(blocks, writer);
                    }
                }
            }
            applyBlocks(blocks);
        } catch (Exception e) {
            System.err.println("[ArcanaQuestTweaks] Failed to load comfort blocks, using defaults.");
            e.printStackTrace();
            applyBlocks(createBlocksDefaults());
        }
    }

    private static ComfortBlocks createBlocksDefaults() {
        ComfortBlocks blocks = new ComfortBlocks();

        blocks.category_limits.put("hearth", 1);
        blocks.category_limits.put("crafting", 1);
        blocks.category_limits.put("bedding", 1);
        blocks.category_limits.put("seating", 2);
        blocks.category_limits.put("lighting", 3);
        blocks.category_limits.put("study", 2);
        blocks.category_limits.put("decoration", 4);
        blocks.category_limits.put("nature", 3);
        blocks.category_limits.put("structure", 8);
        blocks.category_limits.put("pets", 2);

        Map<String, Float> hearth = new LinkedHashMap<>();
        hearth.put("farmersdelight:stove", 4.0f);
        blocks.categories.put("hearth", hearth);

        blocks.categories.put("crafting", defaultCraftingBlocks());

        Map<String, Float> bedding = new LinkedHashMap<>();
        bedding.put("comforts:hammock", 3.5f);
        bedding.put("minecraft:bed", 3.0f);
        bedding.put("comforts:sleeping_bag", 2.0f);
        blocks.categories.put("bedding", bedding);

        Map<String, Float> seating = new LinkedHashMap<>();
        seating.put("bibliocraft:seat", 3.0f);
        blocks.categories.put("seating", seating);

        Map<String, Float> lighting = new LinkedHashMap<>();
        lighting.put("saltmod:salt_lamp", 2.0f);
        lighting.put("fancylamps:gothic_lamp", 2.0f);
        lighting.put("rustic:iron_lantern", 2.0f);
        blocks.categories.put("lighting", lighting);

        Map<String, Float> study = new LinkedHashMap<>();
        study.put("bibliocraft:bookcase", 1.5f);
        study.put("inspirations:bookshelf", 1.5f);
        blocks.categories.put("study", study);

        Map<String, Float> decoration = new LinkedHashMap<>();
        decoration.put("minecraft:carpet", 1.0f);
        decoration.put("minecraft:standing_banner", 1.5f);
        decoration.put("minecraft:wall_banner", 1.5f);
        blocks.categories.put("decoration", decoration);

        Map<String, Float> nature = new LinkedHashMap<>();
        nature.put("minecraft:flower_pot", 1.5f);
        nature.put("minecraft:red_flower", 1.0f);
        nature.put("minecraft:yellow_flower", 1.0f);
        blocks.categories.put("nature", nature);

        Map<String, Float> structure = new LinkedHashMap<>();
        structure.put("rustic:slate_chiseled", 1.0f);
        structure.put("earthworks:block_plaster", 1.0f);
        structure.put("earthworks:block_adobe", 1.0f);
        structure.put("earthworks:block_cob", 1.0f);
        blocks.categories.put("structure", structure);

        return blocks;
    }

    private static Map<String, Float> defaultCraftingBlocks() {
        Map<String, Float> crafting = new LinkedHashMap<>();
        crafting.put("minecraft:crafting_table", 3.0f);
        return crafting;
    }

    private static boolean mergeMissingCrafting(ComfortBlocks blocks) {
        if (blocks.category_limits == null) {
            blocks.category_limits = new LinkedHashMap<>();
        }
        if (blocks.categories == null) {
            blocks.categories = new LinkedHashMap<>();
        }

        boolean changed = false;
        if (!blocks.category_limits.containsKey("crafting")) {
            blocks.category_limits.put("crafting", 1);
            changed = true;
        }
        if (!blocks.categories.containsKey("crafting") || blocks.categories.get("crafting") == null) {
            blocks.categories.put("crafting", defaultCraftingBlocks());
            changed = true;
        }
        return changed;
    }

    private static void applySettings(ComfortSettings settings) {
        ComfortSystemHandler.PET_COMFORT_VALUE = settings.pet_comfort_value;
        ComfortSystemHandler.THRESHOLD_HOMESTEAD_1 = settings.threshold_homestead_1;
        ComfortSystemHandler.THRESHOLD_HOMESTEAD_2 = settings.threshold_homestead_2;
        ComfortSystemHandler.THRESHOLD_HOMESTEAD_3 = settings.threshold_homestead_3;
        ComfortSystemHandler.PROMOTE_TICKS = settings.promote_ticks > 0 ? settings.promote_ticks : 1200L;
        ComfortSystemHandler.DAMAGE_COOLDOWN_TICKS = Math.max(0L, settings.damage_cooldown_ticks);
        ComfortSystemHandler.ATTACK_COOLDOWN_TICKS = Math.max(0L, settings.attack_cooldown_ticks);
        ComfortSystemHandler.ENTRY_REQUIRE_CATEGORIES.clear();
        List<String> entryCategories = settings.entry_require_categories != null
            ? settings.entry_require_categories
            : ComfortSettings.defaultEntryCategories();
        for (String category : entryCategories) {
            if (category != null && !category.isEmpty()) {
                ComfortSystemHandler.ENTRY_REQUIRE_CATEGORIES.add(category);
            }
        }

        ComfortSettings.PenaltiesConfig penalties = settings.penalties != null
            ? settings.penalties : ComfortSettings.PenaltiesConfig.defaults();
        ComfortSystemHandler.PENALTIES_ENABLED = penalties.enabled;
        ComfortSystemHandler.TEMP_PENALTY = penalties.temperature != null
            ? penalties.temperature : ComfortSettings.TemperaturePenalty.defaults();
        ComfortSystemHandler.THIRST_PENALTY = penalties.thirst != null
            ? penalties.thirst : ComfortSettings.RatePenalty.thirstDefaults();
        ComfortSystemHandler.HUNGER_PENALTY = penalties.hunger != null
            ? penalties.hunger : ComfortSettings.RatePenalty.hungerDefaults();
        ComfortSystemHandler.HEALTH_PENALTY = penalties.health != null
            ? penalties.health : ComfortSettings.HealthPenalty.defaults();
        ComfortSystemHandler.POTION_PENALTIES = resolvePenaltyEffects(penalties);

        ComfortSettings.BonusesConfig bonuses = settings.bonuses != null
            ? settings.bonuses : ComfortSettings.BonusesConfig.defaults();
        ComfortSystemHandler.BONUSES_ENABLED = bonuses.enabled;
        ComfortSystemHandler.POTION_BONUSES = resolveBonusEffects(bonuses);
    }

    private static List<ComfortSettings.PotionModifier> resolveBonusEffects(ComfortSettings.BonusesConfig bonuses) {
        if (bonuses.effects != null) {
            return bonuses.effects;
        }
        if (bonuses.farmers_delight_comfort != null) {
            List<ComfortSettings.PotionModifier> list = new ArrayList<>();
            list.add(bonuses.farmers_delight_comfort);
            return list;
        }
        return ComfortSettings.defaultBonusEffects();
    }

    private static List<ComfortSettings.PotionModifier> resolvePenaltyEffects(ComfortSettings.PenaltiesConfig penalties) {
        if (penalties.effects != null) {
            return penalties.effects;
        }
        if (penalties.somnia != null && penalties.somnia.enabled) {
            ComfortSettings.SomniaPenalty s = penalties.somnia;
            List<ComfortSettings.PotionModifier> list = new ArrayList<>();
            list.add(ComfortSettings.modifier(s.sleepy_potion, s.sleepy));
            list.add(ComfortSettings.modifier(s.exhausted_potion, s.exhausted));
            list.add(ComfortSettings.modifier(s.fading_potion, s.fading));
            return list;
        }
        if (penalties.somnia != null && !penalties.somnia.enabled) {
            return new ArrayList<>();
        }
        return ComfortSettings.defaultPenaltyEffects();
    }

    private static void applyBlocks(ComfortBlocks blocks) {
        ComfortSystemHandler.CATEGORY_LIMITS.clear();
        if (blocks.category_limits != null) {
            ComfortSystemHandler.CATEGORY_LIMITS.putAll(blocks.category_limits);
        }

        ComfortSystemHandler.COZY_BLOCKS.clear();
        if (blocks.categories == null) {
            return;
        }
        for (Map.Entry<String, Map<String, Float>> categoryEntry : blocks.categories.entrySet()) {
            String category = categoryEntry.getKey();
            Map<String, Float> blocksMap = categoryEntry.getValue();
            if (blocksMap == null) {
                continue;
            }
            for (Map.Entry<String, Float> blockEntry : blocksMap.entrySet()) {
                ComfortSystemHandler.COZY_BLOCKS.put(blockEntry.getKey(),
                    new ComfortSystemHandler.CozyConfig(blockEntry.getValue(), category));
            }
        }
    }
}
