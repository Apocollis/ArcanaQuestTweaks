package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Set;

/**
 * Versioning of the config copy of {@code cultist_battlemages.json}. The bundled default is copied to the config folder
 * only on first run, so a copy written by an older stage keeps its old numbers. The file carries a
 * {@code schemaVersion}; a copy older than {@link #CURRENT} (or with none, as stage 1 wrote) is backed up to
 * {@code .bak} and replaced by the bundled default. A copy at or above the current version is never touched.
 */
public final class LibraryMigration {

    /** Must match {@code schemaVersion} in the bundled resource (a test checks it). */
    public static final int CURRENT = 4;

    private LibraryMigration() {}

    /** @return the file's {@code schemaVersion}, or null when it has none or is unreadable */
    public static Integer readVersion(String json) {
        try {
            JsonElement el = new JsonParser().parse(json);
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                if (o.has("schemaVersion") && o.get("schemaVersion").isJsonPrimitive()) {
                    return o.get("schemaVersion").getAsInt();
                }
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    public static boolean needsUpgrade(Integer found) {
        return found == null || found < CURRENT;
    }

    /** First free backup name: {@code base.bak}, then {@code base.bak2}, {@code base.bak3} ... */
    public static String backupName(String base, Set<String> existing) {
        String name = base + ".bak";
        int n = 2;
        while (existing.contains(name)) {
            name = base + ".bak" + n;
            n++;
        }
        return name;
    }
}
