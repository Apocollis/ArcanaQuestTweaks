package com.apocollis.aqtweaks.portal;

import java.util.HashMap;
import java.util.Map;

public final class PortalLang {

    public static final String TEAR_NAME = "item.aqtweaks.spatial_rift_tear.name";
    public static final String TEAR_NAME_LINKED = "item.aqtweaks.spatial_rift_tear.name_linked";
    public static final String TEAR_LORE = "item.aqtweaks.spatial_rift_tear.lore";
    public static final String TEAR_UNBOUND = "item.aqtweaks.spatial_rift_tear.unbound";
    public static final String TEAR_BOUND = "item.aqtweaks.spatial_rift_tear.bound";
    public static final String TEAR_SHIFT = "item.aqtweaks.spatial_rift_tear.shift";
    public static final String TEAR_SHIFT_HINT = "item.aqtweaks.spatial_rift_tear.shift_hint";
    public static final String WILD_NAME = "item.aqtweaks.spatial_rift_wild.name";
    public static final String WILD_LORE = "item.aqtweaks.spatial_rift_wild.lore";
    public static final String WILD_SHIFT = "item.aqtweaks.spatial_rift_wild.shift";
    public static final String WILD_SHIFT_HINT = "item.aqtweaks.spatial_rift_wild.shift_hint";

    private static final Map<String, String> VALUES = new HashMap<>();

    private PortalLang() {}

    public static void register() {
        put(TEAR_NAME, "Arcane Tunnel");
        put(TEAR_NAME_LINKED, "Linked Arcane Tunnel");
        put(TEAR_LORE,
                "\u00a7dPulls the arcane threads between places to open a temporary tunnel between them. Attune to a destination to open a rift back to that point.");
        put(TEAR_UNBOUND, "\u00a77Unbound. Right-click in air to mark this place.");
        put(TEAR_BOUND, "Bound: %d, %d, %d (dim %d)");
        put(TEAR_SHIFT,
                "\u00a78Sneak-use unbinds. Air-use attunes this place (unbound) or opens a 60s rift at the bound point (linked). Standing pets and leads follow; sitting pets stay.");
        put(TEAR_SHIFT_HINT, "\u00a78Hold Shift for details.");
        put("item.aqtweaks.spatial_rift_tear.attuned",
                "\u00a75[Arcana]\u00a7r \u00a7dAttuned Arcane Tunnel to X: %d, Y: %d, Z: %d.");
        put("item.aqtweaks.spatial_rift_tear.cleared", "\u00a75[Arcana]\u00a7r \u00a77Cleared Arcane Tunnel attunement.");
        put("item.aqtweaks.spatial_rift_tear.failed", "\u00a75[Arcana]\u00a7r The rift failed to open.");
        put("item.aqtweaks.spatial_rift_tear.missing_dim", "\u00a75[Arcana]\u00a7r The bound dimension is not loaded.");
        put(WILD_NAME, "Unstable Arcane Tunnel");
        put(WILD_LORE,
                "\u00a7dDistorts arcane energy to open a tunnel between the current location and another random point in the same dimension.");
        put(WILD_SHIFT,
                "\u00a78Air-use opens a red rift pair 4000-6000 blocks away. Standing pets and leads follow; sitting pets stay.");
        put(WILD_SHIFT_HINT, "\u00a78Hold Shift for details.");
        put("item.aqtweaks.spatial_rift_wild.no_land", "\u00a75[Arcana]\u00a7r No safe land was found.");
        put("entity.arcane_rift.name", "Arcane Rift");
        put("effect.aqtweaks.homestead", "Homestead");
    }

    public static String format(String key, Object... args) {
        String pattern = VALUES.get(key);
        if (pattern == null) {
            return key;
        }
        return args.length == 0 ? pattern : String.format(pattern, args);
    }

    private static void put(String key, String value) {
        VALUES.put(key, value);
    }
}
