package com.apocollis.aqtweaks.starter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Pure JSON reading and writing of {@link StarterSettings}; fills the blanks a hand-edited file may leave. */
public final class StarterSettingsIO {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private StarterSettingsIO() {}

    /** @throws JsonParseException when the text is not valid JSON of the expected shape */
    public static StarterSettings parse(String json) {
        StarterSettings s = GSON.fromJson(json, StarterSettings.class);
        if (s == null) {
            throw new JsonParseException("empty file");
        }
        return normalize(s);
    }

    public static String toJson(StarterSettings settings) {
        return GSON.toJson(settings);
    }

    static StarterSettings normalize(StarterSettings s) {
        if (s.intro == null) s.intro = "";
        if (s.prompt == null || s.prompt.isEmpty()) s.prompt = StarterDefaults.PROMPT;
        if (s.resetIntro == null || s.resetIntro.isEmpty()) s.resetIntro = StarterDefaults.RESET_INTRO;
        if (s.resetPrompt == null || s.resetPrompt.isEmpty()) s.resetPrompt = StarterDefaults.RESET_PROMPT;
        if (s.revealHoldSeconds < 0) s.revealHoldSeconds = 0;
        if (s.classes == null) s.classes = new ArrayList<>();
        for (StarterClassDef d : s.classes) {
            if (d == null) continue;
            if (d.id == null) d.id = "";
            d.id = d.id.trim();
            if (d.name == null || d.name.trim().isEmpty()) d.name = d.id;
            if (d.flavor == null) d.flavor = "";
            if (d.icon == null) d.icon = "";
            if (d.csgKit == null) d.csgKit = "";
            if (d.perk == null) d.perk = "";
            if (d.skills == null) d.skills = new LinkedHashMap<>();
            if (d.commands == null) d.commands = new ArrayList<>();
        }
        return s;
    }
}
