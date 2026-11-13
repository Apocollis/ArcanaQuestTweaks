package com.apocollis.aqtweaks.twilightforest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure matching and config validation for the Void Breach casing. No Minecraft types, so it is unit tested.
 * Faces are always in the order UP, DOWN, NORTH, SOUTH, EAST, WEST; the first two take the cap block.
 */
public final class VoidBreachShell {
    private VoidBreachShell() {}

    public static final String DEFAULT_CASING = "thaumcraft:metal_void";
    public static final String OCTINE_BLOCK = "thebetweenlands:octine_block";
    public static final String REACTOR_BLOCK = "twilightforest:tower_device";
    public static final int FACE_COUNT = 6;

    /** A block name with an optional metadata; {@code meta < 0} matches any metadata. */
    public static final class BlockSpec {
        public final String name;
        public final int meta;

        BlockSpec(String name, int meta) {
            this.name = name;
            this.meta = meta;
        }

        public boolean matches(String blockName, int blockMeta) {
            return name.equals(blockName) && (meta < 0 || meta == blockMeta);
        }
    }

    public static final class Resolved {
        public final BlockSpec casing;
        public final BlockSpec cap;
        public final List<String> warnings = new ArrayList<>();

        Resolved(BlockSpec casing, BlockSpec cap) {
            this.casing = casing;
            this.cap = cap;
        }
    }

    /** Parses {@code modid:block} or {@code modid:block:meta}. Returns null for anything else. */
    public static BlockSpec parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;
        String[] parts = s.split(":", -1);
        if (parts.length < 2 || parts.length > 3 || parts[0].isEmpty() || parts[1].isEmpty()) return null;
        int meta = -1;
        if (parts.length == 3) {
            try {
                meta = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (meta < 0 || meta > 15) return null;
        }
        return new BlockSpec(parts[0] + ":" + parts[1], meta);
    }

    /** Names a shell may never use: air, the reactor itself, and the Phlogiston ritual's Octine block. */
    public static boolean isBanned(String blockName) {
        return blockName.equals("minecraft:air") || blockName.equals(REACTOR_BLOCK) || blockName.equals(OCTINE_BLOCK);
    }

    /**
     * Resolves the config. A bad or banned casing falls back to Void Metal; an empty cap means the casing; a bad or
     * banned cap falls back to the casing. Each fallback adds one warning.
     */
    public static Resolved resolve(String casingRaw, String capRaw) {
        List<String> warnings = new ArrayList<>();
        BlockSpec casing = parse(casingRaw);
        if (casing == null || isBanned(casing.name)) {
            warnings.add("Casing Block \"" + casingRaw + "\" is not a usable block; using " + DEFAULT_CASING);
            casing = parse(DEFAULT_CASING);
        }
        BlockSpec cap = casing;
        if (capRaw != null && !capRaw.trim().isEmpty()) {
            BlockSpec parsed = parse(capRaw);
            if (parsed == null || isBanned(parsed.name)) {
                warnings.add("Cap Block \"" + capRaw + "\" is not a usable block; using the casing block");
            } else {
                cap = parsed;
            }
        }
        Resolved r = new Resolved(casing, cap);
        r.warnings.addAll(warnings);
        return r;
    }

    /** All six faces match: the first two against the cap, the other four against the casing. */
    public static boolean matches(String[] names, int[] metas, Resolved r) {
        if (names == null || metas == null || names.length != FACE_COUNT || metas.length != FACE_COUNT) return false;
        for (int i = 0; i < FACE_COUNT; i++) {
            BlockSpec spec = i < 2 ? r.cap : r.casing;
            if (names[i] == null || !spec.matches(names[i], metas[i])) return false;
        }
        return true;
    }

    /** All six faces are the Octine block (the Phlogiston ritual's shell). */
    public static boolean isOctineShell(String[] names) {
        if (names == null || names.length != FACE_COUNT) return false;
        for (String n : names) {
            if (!OCTINE_BLOCK.equals(n)) return false;
        }
        return true;
    }
}
