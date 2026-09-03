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
 * {@code mobs[id]} is {@code ATTACK_DAMAGE} base; held weapons and Strength still stack.
 */
public final class GaiaDamageConfig {

    public static final String FILE_NAME = "gaia_mob_damage.json";

    public float spellMultiplier = 1.0f;
    public float bombMultiplier = 1.0f;
    public Map<String, Float> mobs = new LinkedHashMap<>();

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
        if (registryId == null || mobs == null) return null;
        return mobs.get(registryId);
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
        for (Map.Entry<String, Float> e : defaults.mobs.entrySet()) {
            if (!parsed.mobs.containsKey(e.getKey())) {
                parsed.mobs.put(e.getKey(), e.getValue());
                changed = true;
            }
        }
        return changed;
    }

    static GaiaDamageConfig createDefaults() {
        GaiaDamageConfig c = new GaiaDamageConfig();
        c.spellMultiplier = 1.0f;
        c.bombMultiplier = 1.0f;
        // Gaia 1.7.2 100% tier constants: T1=4, T2=8, T3=12
        putAll(c, 4f,
                "ant", "ant_ranger", "arachne", "bee", "cecaelia", "centaur", "cobble_golem", "creep",
                "deathword", "dryad", "dullahan", "ender_eye", "goblin", "goblin_feral", "gryphon",
                "harpy", "harpy_wizard", "hunter", "butler", "illager_inquisitor", "kikimora", "kobold",
                "matango", "cyclops", "mummy", "oni", "orc", "satyress", "selkie", "siren", "sludge_girl",
                "sporeling", "succubus", "toad", "werecat", "wither_cow");
        putAll(c, 8f,
                "anubis", "banshee", "baphomet", "beholder", "bone_knight", "cobblestone_golem", "dhampir",
                "dwarf", "ender_dragon_girl", "flesh_lich", "gelatinous_slime", "illager_fire", "mermaid",
                "minotaurus", "naga", "nine_tails", "shaman", "sharko", "spriggan", "witch", "yeti", "yuki-onna");
        putAll(c, 12f, "minotaur", "sphinx", "valkyrie", "vampire");
        return c;
    }

    private static void putAll(GaiaDamageConfig c, float dmg, String... paths) {
        for (String path : paths) {
            c.mobs.put("grimoireofgaia:" + path, dmg);
        }
    }
}
