package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import java.util.HashSet;
import java.util.Set;

/**
 * The configured allowlist of perks a player may switch on and off ({@code Toggleable Perks}). Only Tweaks-implemented
 * perks (the {@code aqtweaks:} namespace, every effect of which reads {@link PerkAccess}) qualify; anything else in the
 * list is ignored with one warning. Pure, no Minecraft client or Reskillable types.
 */
public final class ToggleablePerks {

    private static String[] cachedSource;
    private static Set<String> cached = new HashSet<>();

    private ToggleablePerks() {}

    public static boolean isToggleable(String registryId) {
        return registryId != null && ids().contains(registryId);
    }

    /** The accepted ids, rebuilt whenever the config array instance changes (a config reload replaces it). */
    public static synchronized Set<String> ids() {
        String[] source = ArcanaQuestTweaksConfig.ReskillableModuleConfig.general.toggleablePerks;
        if (source != cachedSource) {
            cachedSource = source;
            cached = filter(source);
        }
        return cached;
    }

    /** Keeps only valid {@code aqtweaks:} ids; the rest are dropped (the caller logs them). */
    public static Set<String> filter(String[] raw) {
        Set<String> out = new HashSet<>();
        if (raw == null) return out;
        for (String id : raw) {
            if (id == null) continue;
            String trimmed = id.trim();
            if (trimmed.startsWith("aqtweaks:") && trimmed.length() > "aqtweaks:".length()) out.add(trimmed);
        }
        return out;
    }
}
