package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a CraftTweaker bracket line such as {@code <thaumcraft:focus_2>.withTag({...})} into the item id and an
 * NBT string {@code JsonToNBT} can parse (the {@code 1.0 as float} casts become {@code 1.0f}).
 */
public final class CtFocusLine {

    private static final Pattern LINE = Pattern.compile("^\\s*<([^>]+)>(?:\\s*\\.\\s*withTag\\(\\s*(.*)\\s*\\))?\\s*$", Pattern.DOTALL);
    private static final Pattern AS_FLOAT = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s+as\\s+float");
    private static final Pattern AS_DOUBLE = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s+as\\s+double");
    private static final Pattern AS_INT = Pattern.compile("(-?\\d+)\\s+as\\s+int");

    public final String itemId;
    /** NBT in vanilla SNBT syntax, or null when the line had no tag. */
    public final String nbt;

    private CtFocusLine(String itemId, String nbt) {
        this.itemId = itemId;
        this.nbt = nbt;
    }

    /** @return the parsed line, or null if it is not a bracket line */
    public static CtFocusLine parse(String line) {
        if (line == null) {
            return null;
        }
        Matcher m = LINE.matcher(line.trim());
        if (!m.matches()) {
            return null;
        }
        String id = m.group(1).trim();
        String nbt = m.group(2);
        if (nbt != null) {
            nbt = sanitize(nbt.trim());
        }
        return new CtFocusLine(id, nbt);
    }

    static String sanitize(String nbt) {
        String s = AS_FLOAT.matcher(nbt).replaceAll("$1f");
        s = AS_DOUBLE.matcher(s).replaceAll("$1d");
        s = AS_INT.matcher(s).replaceAll("$1");
        return s;
    }
}
