package com.apocollis.aqtweaks.starter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads {@code config/arcanaquesttweaks/aqtweaks_starter_classes.json}, validates it against the running game and
 * keeps the classes that passed. Loaded lazily on first use because skills, perks and CSG kits only exist once the
 * game has finished loading. Server side only; the client never reads the file.
 */
public final class StarterClassRegistry {

    private static final Logger LOG = LogManager.getLogger("aqtweaks");
    private static final String FILE_NAME = "aqtweaks_starter_classes.json";

    private static StarterSettings settings = new StarterSettings();
    private static List<StarterClassDef> valid = new ArrayList<>();
    private static List<String> errors = new ArrayList<>();
    private static boolean loaded;

    private StarterClassRegistry() {}

    public static synchronized void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    /** Re-reads and re-validates the file. @return the error lines (also logged), empty when everything passed */
    public static synchronized List<String> reload() {
        List<String> problems = new ArrayList<>();
        StarterSettings read = readFile(problems);
        StarterClassValidator.Result result = new StarterClassValidator.Result();
        if (read != null) {
            result = StarterClassValidator.validate(read.classes, new StarterEnvironment());
            problems.addAll(result.errors);
        } else {
            read = new StarterSettings();
        }
        settings = read;
        valid = new ArrayList<>(result.valid);
        errors = problems;
        loaded = true;
        for (String line : problems) {
            LOG.error("[starter classes] {}", line);
        }
        if (valid.isEmpty()) {
            LOG.error("[starter classes] no valid class: the menu and the lockdown are off until the config is fixed");
        } else {
            LOG.info("[starter classes] {} class(es) ready: {}", valid.size(), ids());
        }
        return new ArrayList<>(problems);
    }

    private static StarterSettings readFile(List<String> problems) {
        File dir = new File(Loader.instance().getConfigDir(), "arcanaquesttweaks");
        File file = new File(dir, FILE_NAME);
        try {
            if (!file.exists()) {
                dir.mkdirs();
                StarterSettings defaults = StarterDefaults.create();
                Files.write(file.toPath(), StarterSettingsIO.toJson(defaults).getBytes(StandardCharsets.UTF_8));
                return defaults;
            }
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return StarterSettingsIO.parse(text);
        } catch (IOException | RuntimeException e) {
            problems.add("cannot read " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public static synchronized StarterSettings settings() {
        return settings;
    }

    public static synchronized List<StarterClassDef> valid() {
        return Collections.unmodifiableList(new ArrayList<>(valid));
    }

    public static synchronized List<String> errors() {
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static synchronized boolean hasValid() {
        return !valid.isEmpty();
    }

    public static synchronized StarterClassDef find(String id) {
        if (id == null) return null;
        for (StarterClassDef d : valid) {
            if (d.id.equals(id)) return d;
        }
        return null;
    }

    public static synchronized List<StarterClassView> views() {
        List<StarterClassView> out = new ArrayList<>();
        for (StarterClassDef d : valid) {
            out.add(new StarterClassView(d.id, d.name, d.flavor, d.icon));
        }
        return out;
    }

    private static String ids() {
        StringBuilder sb = new StringBuilder();
        for (StarterClassDef d : valid) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(d.id);
        }
        return sb.toString();
    }
}
