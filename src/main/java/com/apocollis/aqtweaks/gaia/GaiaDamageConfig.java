package com.apocollis.aqtweaks.gaia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Instance JSON {@code config/arcanaquesttweaks/gaia_mob_damage.json}. Loaded once in preInit.
 * {@code mobs[id]} is {@code ATTACK_DAMAGE} base; {@code health}/{@code armor} are max health and
 * armor bases. Held weapons and Strength still stack on attack.
 */
public final class GaiaDamageConfig {

    public static final String FILE_NAME = "gaia_mob_damage.json";

    public float spellMultiplier = 1.0f;
    public float bombMultiplier = 1.0f;
    public Map<String, Float> mobs = new LinkedHashMap<>();
    public Map<String, Float> health = new LinkedHashMap<>();
    public Map<String, Float> armor = new LinkedHashMap<>();

    private static GaiaDamageConfig loaded = new GaiaDamageConfig();

    public GaiaDamageConfig() {}

    public static GaiaDamageConfig get() {
        return loaded;
    }

    public static void load(File configDir) {
        File subDir = new File(configDir, "arcanaquesttweaks");
        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        File configFile = new File(subDir, FILE_NAME);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (!configFile.exists()) {
                loaded = createDefaults();
                try (FileWriter writer = new FileWriter(configFile)) {
                    gson.toJson(loaded, writer);
                }
            } else {
                try (FileReader reader = new FileReader(configFile)) {
                    GaiaDamageConfig parsed = gson.fromJson(reader, GaiaDamageConfig.class);
                    if (parsed == null) {
                        loaded = createDefaults();
                    } else {
                        if (parsed.mobs == null) {
                            parsed.mobs = new LinkedHashMap<>();
                        }
                        if (parsed.health == null) {
                            parsed.health = new LinkedHashMap<>();
                        }
                        if (parsed.armor == null) {
                            parsed.armor = new LinkedHashMap<>();
                        }
                        if (mergeMissingMobs(parsed)) {
                            try (FileWriter writer = new FileWriter(configFile)) {
                                gson.toJson(parsed, writer);
                            }
                        }
                        loaded = parsed;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ArcanaQuestTweaks] Failed to load gaia_mob_damage.json, using defaults.");
            e.printStackTrace();
            loaded = createDefaults();
        }
    }

    public Float damageFor(String registryId) {
        return lookup(mobs, registryId);
    }

    public Float healthFor(String registryId) {
        return lookup(health, registryId);
    }

    public Float armorFor(String registryId) {
        return lookup(armor, registryId);
    }

    private static Float lookup(Map<String, Float> map, String registryId) {
        if (registryId == null || map == null) {
            return null;
        }
        return map.get(registryId);
    }

    private static boolean mergeMissingMobs(GaiaDamageConfig parsed) {
        boolean changed = false;
        GaiaDamageConfig defaults = createDefaults();
        if (parsed.spellMultiplier <= 0) {
            parsed.spellMultiplier = defaults.spellMultiplier;
            changed = true;
        }
        if (parsed.bombMultiplier <= 0) {
            parsed.bombMultiplier = defaults.bombMultiplier;
            changed = true;
        }
        changed |= mergeMap(parsed.mobs, defaults.mobs);
        changed |= mergeMap(parsed.health, defaults.health);
        changed |= mergeMap(parsed.armor, defaults.armor);
        return changed;
    }

    private static boolean mergeMap(Map<String, Float> dest, Map<String, Float> defaults) {
        boolean changed = false;
        for (Map.Entry<String, Float> e : defaults.entrySet()) {
            if (!dest.containsKey(e.getKey())) {
                dest.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        return changed;
    }

    static GaiaDamageConfig createDefaults() {
        GaiaDamageConfig c = new GaiaDamageConfig();
        c.spellMultiplier = 1.0f;
        c.bombMultiplier = 1.0f;
        // Gaia 1.7.2 100% attack constants: T1=4, T2=8, T3=12
        putDamage(c, 4f,
                "ant", "ant_ranger", "arachne", "bee", "cecaelia", "centaur", "cobble_golem", "creep",
                "deathword", "dryad", "dullahan", "ender_eye", "goblin", "goblin_feral", "gryphon",
                "harpy", "harpy_wizard", "hunter", "butler", "illager_inquisitor", "kikimora", "kobold",
                "matango", "cyclops", "mummy", "oni", "orc", "satyress", "selkie", "siren", "sludge_girl",
                "sporeling", "succubus", "toad", "werecat", "wither_cow");
        putDamage(c, 8f,
                "anubis", "banshee", "baphomet", "beholder", "bone_knight", "cobblestone_golem", "dhampir",
                "dwarf", "ender_dragon_girl", "flesh_lich", "gelatinous_slime", "illager_fire", "mermaid",
                "minotaurus", "naga", "nine_tails", "shaman", "sharko", "spriggan", "witch", "yeti", "yuki-onna");
        putDamage(c, 12f, "minotaur", "sphinx", "valkyrie", "vampire");
        c.mobs.put("aqtweaks:deep_dwarf", 10f);

        // Pack Gaia tierNmaxHealth=75 → 30/60/120. Armor unscaled 4/8/12.
        putStats(c, 30f, 4f,
                "ant", "ant_ranger", "arachne", "bee", "cecaelia", "centaur", "cobble_golem", "creep",
                "deathword", "dryad", "dullahan", "ender_eye", "goblin", "gryphon",
                "harpy", "harpy_wizard", "hunter", "kikimora", "kobold",
                "matango", "cyclops", "mummy", "oni", "orc", "satyress", "selkie", "siren", "sludge_girl",
                "succubus", "toad", "werecat", "wither_cow");
        putStats(c, 15f, 4f, "goblin_feral", "butler", "illager_inquisitor");
        putStats(c, 15f, 2f, "sporeling");
        putStats(c, 60f, 8f,
                "anubis", "banshee", "baphomet", "beholder", "bone_knight", "cobblestone_golem", "dhampir",
                "dwarf", "ender_dragon_girl", "flesh_lich", "gelatinous_slime", "illager_fire", "mermaid",
                "minotaurus", "naga", "nine_tails", "shaman", "sharko", "spriggan", "witch", "yeti", "yuki-onna");
        putStats(c, 120f, 12f, "minotaur", "sphinx", "valkyrie", "vampire");
        c.health.put("aqtweaks:deep_dwarf", 60f);
        c.armor.put("aqtweaks:deep_dwarf", 8f);
        return c;
    }

    private static void putDamage(GaiaDamageConfig c, float dmg, String... paths) {
        for (String path : paths) {
            c.mobs.put("grimoireofgaia:" + path, dmg);
        }
    }

    private static void putStats(GaiaDamageConfig c, float hp, float armor, String... paths) {
        for (String path : paths) {
            String id = "grimoireofgaia:" + path;
            c.health.put(id, hp);
            c.armor.put(id, armor);
        }
    }
}
