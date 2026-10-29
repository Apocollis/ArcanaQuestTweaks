package com.apocollis.aqtweaks.spawning.table;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.regex.Pattern;

/**
 * Reads the pack's JSON the way its Python scripts do: line comments, block comments and trailing commas are
 * dropped before parsing, so the same files load in both.
 */
public final class LenientJson {
    private static final Pattern LINE_COMMENT = Pattern.compile("//.*$", Pattern.MULTILINE);
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern TRAILING_COMMA = Pattern.compile(",\\s*([\\]}])");

    private LenientJson() {
    }

    public static JsonElement parse(String text) {
        String clean = LINE_COMMENT.matcher(text).replaceAll("");
        clean = BLOCK_COMMENT.matcher(clean).replaceAll("");
        clean = TRAILING_COMMA.matcher(clean).replaceAll("$1");
        return new JsonParser().parse(clean);
    }
}
