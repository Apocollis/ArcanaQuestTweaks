package com.apocollis.aqtweaks.titles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure logic for structure titles: which structure names are asked, how a name becomes a packet key and a
 * language key, and when a player entering or leaving a structure should produce a message. No Minecraft
 * types, so it is unit tested.
 */
public final class StructureTitles {

    /** Key of a player home in the packet. The title text of a home is the registry's formatted name. */
    public static final String HOME_KEY = "home";
    /** The Roguelike Dungeons structure name, answered by that mod's own vanilla-test hook. */
    public static final String ROGUELIKE = "RoguelikeDungeon";
    public static final String LANG_PREFIX = "aqtweaks.structure.";

    /**
     * Vanilla structure names in the order they are asked. The specific scattered features come before the
     * generic {@code Temple}, so a generator that answers them separately wins.
     */
    public static final List<String> VANILLA = List.of(
            "Village", "Mineshaft", "Mansion", "Stronghold", "Monument", "Fortress", "EndCity",
            "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Temple");

    private StructureTitles() {}

    /**
     * The names to ask the world's structure test after a home has been ruled out: Roguelike dungeons, the
     * vanilla list, then the configured extras. Blank names, duplicates (ignoring case) and the home name are
     * dropped.
     */
    public static List<String> names(List<String> extras) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        add(out, seen, ROGUELIKE);
        for (String name : VANILLA) {
            add(out, seen, name);
        }
        if (extras != null) {
            for (String extra : extras) {
                add(out, seen, extra);
            }
        }
        return out;
    }

    private static void add(List<String> out, Set<String> seen, String name) {
        if (name == null) {
            return;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(HOME_KEY)) {
            return;
        }
        if (seen.add(trimmed.toLowerCase(Locale.ROOT))) {
            out.add(trimmed);
        }
    }

    /** The key that travels in the packet: the structure name in lower case. */
    public static String keyOf(String structureName) {
        return structureName == null ? "" : structureName.trim().toLowerCase(Locale.ROOT);
    }

    /** Language key of a structure name, for example {@code aqtweaks.structure.desert_pyramid}. */
    public static String langKey(String key) {
        return LANG_PREFIX + keyOf(key);
    }

    /**
     * Readable fallback for a name with no language entry: underscores become spaces and a lower-to-upper
     * case change gets a space, so {@code EndCity} is "End City" and {@code Desert_Pyramid} "Desert Pyramid".
     */
    public static String prettify(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String source = name.trim();
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '_' || c == '-') {
                sb.append(' ');
                continue;
            }
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(source.charAt(i - 1))) {
                sb.append(' ');
            }
            if (sb.length() == 0 || sb.charAt(sb.length() - 1) == ' ') {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Title shown on the client. A home uses the server's formatted name (or plain "Home" when the owner name
     * is hidden); everything else uses its language entry when there is one, else the prettified name. For a
     * structure that is not a home, {@code serverTitle} carries the structure name in its original case.
     */
    public static String clientTitle(String key, String serverTitle, String translated, boolean showOwnerName,
            String plainHome) {
        if (HOME_KEY.equals(key)) {
            if (showOwnerName && serverTitle != null && !serverTitle.isEmpty()) {
                return serverTitle;
            }
            return plainHome;
        }
        if (translated != null && !translated.isEmpty() && !translated.equals(langKey(key))) {
            return translated;
        }
        return prettify(serverTitle != null && !serverTitle.isBlank() ? serverTitle : key);
    }

    // ----- change detection ------------------------------------------------------------------------------

    public enum Kind { NONE, ENTER, EXIT }

    /** What to tell a client: nothing, a structure entered, or the structure just left. */
    public static final class Transition {
        public final Kind kind;
        public final String key;

        Transition(Kind kind, String key) {
            this.kind = kind;
            this.key = key;
        }

        @Override
        public String toString() {
            return kind + (key == null ? "" : ":" + key);
        }
    }

    private static final Transition NONE = new Transition(Kind.NONE, null);

    /**
     * Per-player memory of the last structure and when each key was last announced. A re-entry within
     * {@code repeatCooldownTicks} of the same key's last announcement is remembered but not announced, so a
     * player walking along a structure's edge is not spammed.
     */
    public static final class Tracker {
        private final Map<UUID, String> last = new HashMap<>();
        private final Map<UUID, Map<String, Long>> announced = new HashMap<>();

        public Transition update(UUID player, String currentKey, long now, int repeatCooldownTicks) {
            String previous = last.get(player);
            String current = currentKey == null || currentKey.isEmpty() ? null : currentKey;
            if (equal(previous, current)) {
                return NONE;
            }
            if (current == null) {
                last.remove(player);
                return new Transition(Kind.EXIT, previous);
            }
            last.put(player, current);
            Map<String, Long> times = announced.computeIfAbsent(player, id -> new HashMap<>());
            Long at = times.get(current);
            if (at != null && now - at < repeatCooldownTicks && now >= at) {
                return NONE;
            }
            times.put(current, now);
            return new Transition(Kind.ENTER, current);
        }

        public void forget(UUID player) {
            last.remove(player);
            announced.remove(player);
        }

        public String lastKey(UUID player) {
            return last.get(player);
        }
    }

    private static boolean equal(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
