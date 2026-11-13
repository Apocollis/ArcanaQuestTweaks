package com.apocollis.aqtweaks.paragon;

import java.util.EnumSet;
import java.util.Locale;

/** The nine Tartarite items that Metallurgy lets take a Paragon. No other item is ever eligible. */
public enum TartariteKind {
    SWORD(false), PICKAXE(false), AXE(false), SHOVEL(false), HOE(false),
    HELMET(true), CHESTPLATE(true), LEGGINGS(true), BOOTS(true);

    public static final String NAMESPACE = "metallurgy";
    private static final String PREFIX = "tartarite_";

    private final boolean armor;

    TartariteKind(boolean armor) {
        this.armor = armor;
    }

    public boolean isArmor() {
        return armor;
    }

    /** True for the pickaxe, axe, shovel and hoe. */
    public boolean isTool() {
        return this == PICKAXE || this == AXE || this == SHOVEL || this == HOE;
    }

    /** True for anything held in the hand to hit with, including the tools. */
    public boolean isHandheld() {
        return !armor;
    }

    public String itemPath() {
        return PREFIX + name().toLowerCase(Locale.ROOT);
    }

    /** The registry path ({@code tartarite_sword}) for a kind, or {@code null} for a path that is not one of the nine. */
    public static TartariteKind fromPath(String namespace, String path) {
        if (!NAMESPACE.equals(namespace) || path == null || !path.startsWith(PREFIX)) return null;
        String tail = path.substring(PREFIX.length());
        for (TartariteKind kind : values()) {
            if (kind.name().toLowerCase(Locale.ROOT).equals(tail)) return kind;
        }
        return null;
    }

    public static EnumSet<TartariteKind> weaponsAndTools() {
        return EnumSet.of(SWORD, PICKAXE, AXE, SHOVEL);
    }

    public static EnumSet<TartariteKind> armorPieces() {
        return EnumSet.of(HELMET, CHESTPLATE, LEGGINGS, BOOTS);
    }
}
